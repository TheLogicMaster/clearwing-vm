/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.thelogicmaster.clearwing.translator.bytecodes;

import com.thelogicmaster.clearwing.translator.ByteCodeClass;
import com.thelogicmaster.clearwing.translator.ByteCodeMethodArg;
import com.thelogicmaster.clearwing.translator.BytecodeMethod;
import com.thelogicmaster.clearwing.translator.Parser;
import com.thelogicmaster.clearwing.translator.SignatureSet;
import com.thelogicmaster.clearwing.translator.Util;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;

/**
 *
 * @author shannah
 */
public class CustomInvoke extends Instruction {
    private String owner;
    private String name;
    private String desc;
    private boolean itf;
    private String[] literalArgs;
    private int origOpcode;
    private String targetObjectLiteral;
    private boolean noReturn;
    
    
    
    
    public CustomInvoke(int opcode, String owner, String name, String desc, boolean itf) {
        super(-1);
        this.origOpcode = opcode;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.itf = itf;
    }
    
    private String cMethodName;
    private String getCMethodName() {
        if (cMethodName == null) {
            cMethodName = name.replace('-', '_');
        }
        return cMethodName;
    }
    
    public void setTargetObjectLiteral(String lit) {
        this.targetObjectLiteral = lit;
        
    }
    
    public String getTargetObjectLiteral() {
        return targetObjectLiteral;
    }
    
    public static CustomInvoke create(Invoke invoke) {
        return new CustomInvoke(invoke.getOpcode(), invoke.getOwner(), invoke.getName(), invoke.getDesc(), invoke.isItf());
    }

    public boolean isMethodUsed(String desc, String name) {
        return this.desc.equals(desc) && this.name.equals(name);
    }

    public String getMethodUsed() {
        return desc + "." + name;
    }
    public String getMethodUsedName() { return(name); }
    public String getSignature() { return(desc); }
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        String t = owner.replace('.', '_').replace('/', '_').replace('$', '_').replace('-', '_');
        t = unarray(t);
        if(t != null && !dependencyList.contains(t)) {
            dependencyList.add(t);
        }

        StringBuilder bld = new StringBuilder();
        if(origOpcode != Opcodes.INVOKEINTERFACE && origOpcode != Opcodes.INVOKEVIRTUAL) {
            return;
        }         
        bld.append(owner.replace('/', '_').replace('$', '_').replace('-', '_'));
        bld.append("_");
        if(name.equals("<init>")) {
            bld.append("__INIT__");
        } else {
            if(name.equals("<clinit>")) {
                bld.append("__CLINIT__");
            } else {
                bld.append(getCMethodName());
            }
        }
        bld.append("__");
        ArrayList<String> args = new ArrayList<String>();
        String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, bld, args);
        String str = bld.toString();
        BytecodeMethod.addVirtualMethodsInvoked(str);
    }
    
    private String findActualOwner(ByteCodeClass bc) {
        if(bc == null) {
            return owner;
        }
        List<BytecodeMethod> mtds = bc.getMethods();
        if(mtds == null) {
            return owner;
        }
        for(BytecodeMethod mtd : mtds) {
            if(mtd.getMethodName().equals(name) && mtd.isStatic()) {
                return bc.getClsName();
            }
        }
        return findActualOwner(bc.getBaseClassObject());
    }
    
    public boolean methodHasReturnValue() {
        return BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, new StringBuilder(), new ArrayList<String>()) != null;
    }
    
    public String getReturnValue() {
        ArrayList<String> args = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, sb, args);
        return returnVal;
    }
    
    
    public boolean appendExpression(StringBuilder b) {
        // special case for clone on an array which isn't a real method invocation
        if(name.equals("clone") && owner.indexOf('[') > -1) {
            if (targetObjectLiteral != null) {
                b.append("cloneArray(").append(targetObjectLiteral).append(")");
            } else {
                b.append("cloneArray(POP_OBJ(1))");
            }
            return true;
        }
        
        StringBuilder bld = new StringBuilder();
        if(origOpcode == Opcodes.INVOKEINTERFACE || origOpcode == Opcodes.INVOKEVIRTUAL) {
            b.append("    ");
            
            // Well, it is actually legal to call private methods with invoke virtual, and kotlin
            // generates such calls.  But ParparVM strips out these virtual method definitions
            // so we need to check 
            boolean isVirtual = true;
            if (origOpcode == Opcodes.INVOKEVIRTUAL) {
                ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_').replace('-', '_'));
                if (bc == null) {
                    System.err.println("WARNING: Failed to find class object for owner "+owner+" when rendering virtual method "+name);
                } else {
                    if (bc.isMethodPrivate(name, desc)) {
                        isVirtual = false;
                    }
                }
                
            }
            if (isVirtual) {
                bld.append("virtual_");
            }
        } else {
            b.append("    ");
        }
        
        if(origOpcode == Opcodes.INVOKESTATIC) {
            // find the actual class of the static method to workaround javac not defining it correctly
            ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_').replace('-', '_'));
            owner = findActualOwner(bc);
        }
        if (owner.startsWith("[")) {
            bld.append("java_lang_Object");
        } else{
            bld.append(owner.replace('/', '_').replace('$', '_').replace('-', '_'));
        }
        bld.append("_");
        if(name.equals("<init>")) {
            bld.append("__INIT__");
        } else {
            if(name.equals("<clinit>")) {
                bld.append("__CLINIT__");
            } else {
                bld.append(getCMethodName());
            }
        }
        bld.append("__");
        ArrayList<String> args = new ArrayList<String>();
        String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, bld, args);
        int numLiteralArgs = this.getNumLiteralArgs();
        if (numLiteralArgs > 0) {
            b.append("/* CustomInvoke */");
        }
        boolean noPop = false;
        b.append(bld);
        
        b.append("(threadStateData");
        
        
        
        if(origOpcode != Opcodes.INVOKESTATIC) {
            if (targetObjectLiteral == null) {
                //b.append(", SP[-");
                //b.append(args.size() + 1 - numLiteralArgs);
                //b.append("].data.o");
                return false;
            } else {
                b.append(", ").append(targetObjectLiteral);
                numLiteralArgs++;
            }
        }
        //int offset = args.size();
        //int numArgs = offset;
        int argIndex=0;
        for(String a : args) {
            
            b.append(", ");
            if (literalArgs != null && literalArgs[argIndex] != null) {
                b.append(literalArgs[argIndex]);
            } else {
                return false;
                //b.append("SP[-");
                //b.append(offset);
                //b.append("].data.");
                //b.append(a);
                //offset--;
            }
            argIndex++;
        }
        if (returnVal == null) {
            return false;
        }
        
        b.append(")");
        
        return true;
        
    }
    
    
    @Override
    public void appendInstruction(StringBuilder b) {
        // special case for clone on an array which isn't a real method invocation
        if(name.equals("clone") && owner.indexOf('[') > -1) {
            if (targetObjectLiteral != null) {
                b.append("    PUSH_OBJ(cloneArray(").append(targetObjectLiteral).append("));\n");
            } else {
                b.append("    POP_MANY_AND_PUSH_OBJ(cloneArray(PEEK_OBJ(1)), 1);\n");
            }
            return;
        }
        
        StringBuilder bld = new StringBuilder();
        if(origOpcode == Opcodes.INVOKEINTERFACE || origOpcode == Opcodes.INVOKEVIRTUAL) {
            b.append("    ");
            
            // Well, it is actually legal to call private methods with invoke virtual, and kotlin
            // generates such calls.  But ParparVM strips out these virtual method definitions
            // so we need to check 
            boolean isVirtual = true;
            if (origOpcode == Opcodes.INVOKEVIRTUAL) {
                ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_').replace('-', '_'));
                if (bc == null) {
                    System.err.println("WARNING: Failed to find class object for owner "+owner+" when rendering virtual method "+name);
                } else {
                    if (bc.isMethodPrivate(name, desc)) {
                        isVirtual = false;
                    }
                }
                
            }
            if (isVirtual) {
                bld.append("virtual_");
            }
        } else {
            b.append("    ");
        }
        
        if(origOpcode == Opcodes.INVOKESTATIC) {
            // find the actual class of the static method to workaround javac not defining it correctly
            ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_').replace('-', '_'));
            owner = findActualOwner(bc);
        }
        //if(owner.replace('/', '_').replace('$', '_').equals("java_lang_System_1") && name.equals("sleep")) {
        //    System.out.println("Break");
        //}
        if (owner.startsWith("[")) {
            bld.append("java_lang_Object");
        } else{
            bld.append(owner.replace('/', '_').replace('$', '_').replace('-', '_'));
        }
        bld.append("_");
        if(name.equals("<init>")) {
            bld.append("__INIT__");
        } else {
            if(name.equals("<clinit>")) {
                bld.append("__CLINIT__");
            } else {
                bld.append(getCMethodName());
            }
        }
        bld.append("__");
        ArrayList<String> args = new ArrayList<String>();
        String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, bld, args);
        int numLiteralArgs = this.getNumLiteralArgs();
        if (numLiteralArgs > 0) {
            b.append("/* CustomInvoke */");
        }
        boolean noPop = false;
        if(returnVal == null || noReturn) {
            b.append(bld);
        } else {
            if(args.size() - numLiteralArgs == 0 && origOpcode == Opcodes.INVOKESTATIC) {
                // special case for static method
                if(returnVal.equals("JAVA_OBJECT")) {
                    b.append("PUSH_OBJ");
                } else {
                    if(returnVal.equals("JAVA_INT")) {
                        b.append("PUSH_INT");
                    } else {
                        if(returnVal.equals("JAVA_LONG")) {
                            b.append("PUSH_LONG");
                        } else {
                            if(returnVal.equals("JAVA_DOUBLE")) {
                                b.append("PUSH_DOUBLE");
                            } else {
                                if(returnVal.equals("JAVA_FLOAT")) {
                                    b.append("PUSH_FLOAT");
                                } else {
                                    throw new UnsupportedOperationException("Unknown type: " + returnVal);
                                }
                            }
                        }
                    }
                }
                //b.append(returnVal);
                noPop = true;
                b.append("(");
            } else {
                //b.append("POP_MANY_AND_");
                //b.append(returnVal);
                b.append("{ ");
                b.append(returnVal);
                b.append(" tmpResult = ");
            }
            b.append(bld);
        }
        b.append("(threadStateData");
        
        
        
        if(origOpcode != Opcodes.INVOKESTATIC) {
            if (targetObjectLiteral == null) {
                b.append(", SP[-");
                b.append(args.size() + 1 - numLiteralArgs);
                b.append("].data.o");
            } else {
                b.append(", "+targetObjectLiteral);
                numLiteralArgs++;
            }
        }
        int offset = args.size();
        //int numArgs = offset;
        int argIndex=0;
        for(String a : args) {
            
            b.append(", ");
            if (literalArgs != null && literalArgs[argIndex] != null) {
                b.append(literalArgs[argIndex]);
            } else {
                b.append("SP[-");
                b.append(offset);
                b.append("].data.");
                b.append(a);
                offset--;
            }
            argIndex++;
        }
        if(noPop) {
            b.append("));\n");
            return;
        }
        if(returnVal != null && !noReturn) {
            b.append(");\n");
            if(origOpcode != Opcodes.INVOKESTATIC) {
                if(args.size() - numLiteralArgs > 0) {
                    b.append("    SP -= ");
                    b.append(args.size() - numLiteralArgs);
                    b.append(";\n");
                }
            } else {
                if(args.size() - numLiteralArgs > 1) {
                    b.append("    SP -= ");
                    b.append(args.size() - numLiteralArgs - 1);
                    b.append(";\n");
                }
            }
            if (targetObjectLiteral == null) {
                if(returnVal.equals("JAVA_OBJECT")) {
                    b.append("    SP[-1].data.o = tmpResult; SP[-1].type = CN1_TYPE_OBJECT; }\n");
                } else {
                    if(returnVal.equals("JAVA_INT")) {
                        b.append("    SP[-1].data.i = tmpResult; SP[-1].type = CN1_TYPE_INT; }\n");
                    } else {
                        if(returnVal.equals("JAVA_LONG")) {
                            b.append("    SP[-1].data.l = tmpResult; SP[-1].type = CN1_TYPE_LONG; }\n");
                        } else {
                            if(returnVal.equals("JAVA_DOUBLE")) {
                                b.append("    SP[-1].data.d = tmpResult; SP[-1].type = CN1_TYPE_DOUBLE; }\n");
                            } else {
                                if(returnVal.equals("JAVA_FLOAT")) {
                                    b.append("    SP[-1].data.f = tmpResult; SP[-1].type = CN1_TYPE_FLOAT; }\n");
                                } else {
                                    throw new UnsupportedOperationException("Unknown type: " + returnVal);
                                }
                            }
                        }
                    }
                }
            } else {
                if(returnVal.equals("JAVA_OBJECT")) {
                    b.append("    PUSH_OBJ(tmpResult); }\n");
                } else {
                    if(returnVal.equals("JAVA_INT")) {
                        b.append("    PUSH_INT(tmpResult); }\n");
                    } else {
                        if(returnVal.equals("JAVA_LONG")) {
                            b.append("    PUSH_LONG(tmpResult); }\n");
                        } else {
                            if(returnVal.equals("JAVA_DOUBLE")) {
                                b.append("    PUSH_DOUBLE(tmpResult); }\n");
                            } else {
                                if(returnVal.equals("JAVA_FLOAT")) {
                                    b.append("    PUSH_FLOAT(tmpResult); }\n");
                                } else {
                                    throw new UnsupportedOperationException("Unknown type: " + returnVal);
                                }
                            }
                        }
                    }
                }
            }
            
            
            return;
        }
        b.append("); ");
        int val; 
        if(origOpcode != Opcodes.INVOKESTATIC) {
            val = args.size() + 1 - numLiteralArgs;
        } else {
            val = args.size() - numLiteralArgs;
        }
        if(val > 0) {
            b.append("    SP -= ");
            b.append(val);
            b.append(";\n");
        } else {
            b.append("\n");            
        }
    }
    
    
    public List<ByteCodeMethodArg> getArgs() {
        return Util.getMethodArgs(desc);
    }
    
    public void setLiteralArg(int index, String arg) {
        if (literalArgs == null) {
            literalArgs = new String[getArgs().size()];
        }
        if (index >= literalArgs.length) {
            throw new RuntimeException("Attempt to set literal arg "+index+" on method invocation that only takes "+literalArgs.length+" args.  Method: "+owner+"."+name+" "+desc);
        }
        literalArgs[index] = arg;
    }
    
    private int getNumLiteralArgs() {
        if (literalArgs == null) {
            return 0;
        }
        int count = 0;
        for (int i=0; i < literalArgs.length; i++) {
            if (literalArgs[i] != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the noReturn
     */
    public boolean isNoReturn() {
        return noReturn;
    }

    /**
     * @param noReturn the noReturn to set
     */
    public void setNoReturn(boolean noReturn) {
        this.noReturn = noReturn;
    }

    public boolean containsSignature(SignatureSet sig) {
            return desc.equals(sig.getSignature());
    }
    public String getMethodName() {
            return(name);
    }
    
    
    
    
}
