package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class InvokeStringConcatInstruction extends Instruction {
    private final MethodSignature signature;
    private final String recipe;
    private final Object[] constants;
    
    public InvokeStringConcatInstruction(BytecodeMethod method, String desc, String recipe, Object[] constants) {
        super(method, -1);
        signature = new MethodSignature("", desc, null);
        this.recipe = recipe;
        this.constants = constants;
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        int arg = 0;
        int constant = 0;
        char[] chars = recipe.toCharArray();
        int values = 0;
        for (char c : chars)
            if (c == '\u0001' || c == '\u0002')
                values++;
        builder.append("\tPOP_N(").append(values).append("); // Pop string concat args\n");
        builder.append("\t(sp++)->o = (jobject)concatStringsRecipe(ctx, ").append(Utils.encodeStringLiteral(recipe)).append(".string, ").append(values);
        for (char c : chars) {
            if (c == '\u0001') {
                JavaType paramType = signature.getParamTypes()[arg];
                builder.append(", ");
                boolean wrapped = paramType.appendWrapperPrefix(new JavaType("Ljava/lang/Object;"), builder);
                builder.append("sp[").append(arg).append("].").append(paramType.getBasicType().getStackName());
                if (wrapped)
                    builder.append(")");
                arg++;
            } else if (c == '\u0002') {
                builder.append(", (jobject)").append(Utils.encodeStringLiteral((String)constants[constant++]));
            }
        }
        builder.append(");\n");
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        setInputs(signature.getParamTypes());
        setBasicOutputs(TypeVariants.OBJECT);
    }
}
