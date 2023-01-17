package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.TypeVariants;

public interface LocalInstruction {

    TypeVariants getLocalType();

    int getLocal();
}
