import process from 'process';
import { WebSocket } from '../../../guide-vsc/node_modules/ws/wrapper.mjs';

function parseArgs(argv) {
	const options = {
		host: '127.0.0.1',
		port: 8765,
		token: '',
		mode: 'smoke',
		timeoutMs: 15000
	};
	for (let index = 0; index < argv.length; index++) {
		const arg = argv[index];
		const next = argv[index + 1];
		if (arg === '--host' && next) {
			options.host = next;
			index++;
			continue;
		}
		if (arg === '--port' && next) {
			options.port = Number(next);
			index++;
			continue;
		}
		if (arg === '--token' && next) {
			options.token = next;
			index++;
			continue;
		}
		if (arg === '--mode' && next) {
			options.mode = next;
			index++;
			continue;
		}
		if (arg === '--timeoutMs' && next) {
			options.timeoutMs = Number(next);
			index++;
		}
	}
	return options;
}

function createEnvelope(id, method, payload) {
	return {
		id,
		type: 'request',
		method,
		protocol: 1,
		payload
	};
}

function waitForResponse(socket, id, timeoutMs) {
	return new Promise((resolve, reject) => {
		const timer = setTimeout(() => {
			cleanup();
			reject(new Error(`Timed out waiting for response ${id}`));
		}, timeoutMs);
		const onMessage = (raw) => {
			const message = JSON.parse(String(raw));
			if (message.id !== id) {
				return;
			}
			cleanup();
			resolve(message);
		};
		const onError = (error) => {
			cleanup();
			reject(error instanceof Error ? error : new Error(String(error)));
		};
		const cleanup = () => {
			clearTimeout(timer);
			socket.off('message', onMessage);
			socket.off('error', onError);
		};
		socket.on('message', onMessage);
		socket.on('error', onError);
	});
}

async function send(socket, id, method, payload, timeoutMs) {
	socket.send(JSON.stringify(createEnvelope(id, method, payload)));
	return waitForResponse(socket, id, timeoutMs);
}

async function querySemantic(socket, capability, prefix, filters, timeoutMs) {
	const id = `semantic.${capability}.${Date.now()}`;
	const response = await send(socket, id, 'semantic.query', {
		capability,
		cursor: '',
		limit: 20,
		prefix,
		filters
	}, timeoutMs);
	if (response.type !== 'response') {
		throw new Error(`Unexpected semantic response type for ${capability}: ${response.type}`);
	}
	return response.payload;
}

async function validateDocument(socket, timeoutMs) {
	const response = await send(socket, `document.validate.${Date.now()}`, 'document.validate', {
		uri: 'file:///runtime-bridge-smoke.md',
		languageId: 'markdown',
		text: '<Entity id="minecraft:player" />\n'
	}, timeoutMs);
	if (response.type !== 'response') {
		throw new Error(`Unexpected document validation response type: ${response.type}`);
	}
	return response.payload;
}

async function collectBootstrapEntries(socket, capability, timeoutMs) {
	const firstPage = await querySemantic(socket, capability, '', {}, timeoutMs);
	const entries = Array.isArray(firstPage.entries) ? firstPage.entries.slice() : [];
	let nextCursor = typeof firstPage.nextCursor === 'string' && firstPage.nextCursor.length > 0
		? firstPage.nextCursor
		: undefined;
	while (nextCursor && entries.length < 60) {
		const id = `semantic.${capability}.${nextCursor}.${Date.now()}`;
		const response = await send(socket, id, 'semantic.query', {
			capability,
			cursor: nextCursor,
			limit: 20,
			prefix: '',
			filters: {}
		}, timeoutMs);
		const payload = response.payload ?? {};
		if (Array.isArray(payload.entries)) {
			entries.push(...payload.entries);
		}
		nextCursor = typeof payload.nextCursor === 'string' && payload.nextCursor.length > 0
			? payload.nextCursor
			: undefined;
	}
	return {
		capability,
		version: firstPage.version,
		entries,
		nextCursor: firstPage.nextCursor ?? null
	};
}

async function main() {
	const options = parseArgs(process.argv.slice(2));
	if (!options.token) {
		throw new Error('Missing required --token value.');
	}
	const url = `ws://${options.host}:${options.port}`;
	const socket = new WebSocket(url, { maxPayload: 262144 });
	await new Promise((resolve, reject) => {
		const timer = setTimeout(() => reject(new Error(`Timed out connecting to ${url}`)), options.timeoutMs);
		socket.once('open', () => {
			clearTimeout(timer);
			resolve();
		});
		socket.once('error', (error) => {
			clearTimeout(timer);
			reject(error instanceof Error ? error : new Error(String(error)));
		});
	});

	try {
		const hello = await send(socket, 'hello', 'hello', {
			token: options.token,
			clientName: 'guidenh-runtime-bridge-script',
			supportedProtocols: [1]
		}, options.timeoutMs);
		if (hello.type !== 'response') {
			throw new Error(`Handshake failed: ${JSON.stringify(hello)}`);
		}

		const capabilities = await send(socket, 'capabilities', 'capabilities', {}, options.timeoutMs);
		const capabilityList = Array.isArray(capabilities.payload?.capabilities) ? capabilities.payload.capabilities : [];
		const results = {
			hello: hello.payload,
			capabilities: capabilityList
		};

		if (options.mode === 'smoke') {
			results.documentValidate = await validateDocument(socket, options.timeoutMs);
			results.items = await querySemantic(socket, 'items', 'minecraft:stone', {}, options.timeoutMs);
			results.pages = await querySemantic(socket, 'pages', 'index', {}, options.timeoutMs);
			results.entities = {
				zombie: await querySemantic(socket, 'entities', 'z', {}, options.timeoutMs),
				player: await querySemantic(socket, 'entities', 'player', {}, options.timeoutMs),
				upperPlayer: await querySemantic(socket, 'entities', 'Player', {}, options.timeoutMs)
			};
			results.structurelib = await querySemantic(
				socket,
				'structurelib',
				'gregtech',
				{},
				options.timeoutMs
			);
		}

		if (options.mode === 'bootstrap') {
			results.bootstrap = {
				items: await collectBootstrapEntries(socket, 'items', options.timeoutMs),
				pages: await collectBootstrapEntries(socket, 'pages', options.timeoutMs),
				entities: await collectBootstrapEntries(socket, 'entities', options.timeoutMs),
				structurelib: await collectBootstrapEntries(socket, 'structurelib', options.timeoutMs)
			};
		}

		process.stdout.write(`${JSON.stringify(results, null, 2)}\n`);
	} finally {
		socket.close();
	}
}

main().catch((error) => {
	process.stderr.write(`${error instanceof Error ? error.stack ?? error.message : String(error)}\n`);
	process.exitCode = 1;
});
