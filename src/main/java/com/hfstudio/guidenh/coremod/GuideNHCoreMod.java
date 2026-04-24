package com.hfstudio.guidenh.coremod;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.mixins.Mixins;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class GuideNHCoreMod implements IFMLLoadingPlugin, IEarlyMixinLoader {

    static {
        try {
            ModConfig.registerConfig();
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    public GuideNHCoreMod() {}

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.guidenh.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
