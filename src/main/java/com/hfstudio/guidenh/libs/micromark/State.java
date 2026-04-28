package com.hfstudio.guidenh.libs.micromark;

import org.jetbrains.annotations.Nullable;

/**
 * The main unit in the state machine: a function that gets a character code and has certain effects.
 * A state function should return another function: the next state-as-a-function to go to.
 * But there is one case where they return void: for the eof character code (at the end of a value). The reason being:
 * well, there isn’t any state that makes sense, so void works well. Practically that has also helped: if for some
 * reason it was a mistake, then an exception is throw because there is no next function, meaning it surfaces early.
 */
@FunctionalInterface
public interface State {

    @Nullable
    State step(int code);

}
