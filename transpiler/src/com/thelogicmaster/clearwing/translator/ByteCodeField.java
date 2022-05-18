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

package com.thelogicmaster.clearwing.translator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class ByteCodeField {
    private final String clsName;
    private boolean staticField;
    private String fieldName;

    private List<String> dependentClasses = new ArrayList<String>();
    //private List<String> exportedClasses = new ArrayList<String>();
    
    private int arrayDimensions;
    private String type;
    private Class primitiveType;
    private boolean finalField;
    private Object value;
    private boolean privateField;
    private int modifiers;
    private String[] genericTypes;
    private int[] genericTypesDimensions;
    
    public ByteCodeField(String clsName, int access, String name, String desc, String signature, Object value) {
        this.clsName = clsName;
        this.value = value;
        privateField = (access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
        if(value != null && value instanceof String) {
            Parser.addToConstantPool((String)value);
        }
        modifiers = access;
        staticField = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
        finalField = (access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL;
        fieldName = name.replace('$', '_');

        Pattern outerPattern = Pattern.compile(".*<(.+)>;.*");
        Matcher outerMatcher = outerPattern.matcher(signature == null ? "" : signature);
        if (outerMatcher.matches()) {
            String[] types = outerMatcher.group(1).replaceAll("<(.+)>", "").replace("+", "").replace("-", "")
                .replace("$", "_").replace("/", "_").split(";");
            genericTypes = new String[types.length];
            genericTypesDimensions = new int[types.length];
            for (int i = 0; i < types.length; i++) {
                String type = types[i];
                String rawType = type.replace("[", "");
                genericTypes[i] = rawType.startsWith("L") ? rawType.substring(1) : "java_lang_Object";
                genericTypesDimensions[i] = type.length() - rawType.length();
            }
        } else {
            genericTypes = new String[0];
            genericTypesDimensions = new int[0];
        }

        arrayDimensions = 0;
        while(desc.startsWith("[")) {
            desc = desc.substring(1);
            arrayDimensions++;
        }
        char currentType = desc.charAt(0);
        switch(currentType) {
            case 'L':
                // Object skip until ;
                int idx = desc.indexOf(';');
                String objectType = desc.substring(1, idx);
                objectType = objectType.replace('/', '_').replace('$', '_');
                if(!dependentClasses.contains(objectType)) {
                    dependentClasses.add(objectType);
                }
                //if (!privateField && !exportedClasses.contains(objectType)) {
                //    exportedClasses.add(objectType);
                //}
                
                type = objectType;
                break;
            case 'I':
                primitiveType = Integer.TYPE;
                break;
            case 'J':
                primitiveType = Long.TYPE;
                break;
            case 'B':
                primitiveType = Byte.TYPE;
                break;
            case 'S':
                primitiveType = Short.TYPE;
                break;
            case 'F':
                primitiveType = Float.TYPE;
                break;
            case 'D':
                primitiveType = Double.TYPE;
                break;
            case 'Z':
                primitiveType = Boolean.TYPE;
                break;
            case 'C':
                primitiveType = Character.TYPE;
                break;
        }
    }

    public String[] getGenericTypes () {
        return genericTypes;
    }

    public int[] getGenericTypesDimensions () {
        return genericTypesDimensions;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Class<?> getPrimitiveType() {
        return primitiveType;
    }

    public String getFieldName() {
        return fieldName;
    }
    
    public String getCDefinition() {
        if(type != null || arrayDimensions > 0) {
            return "JAVA_OBJECT";
        }
        return Util.getCType(primitiveType);
    }

    public void appendClassType(StringBuilder b) {
        b.append("&class");
        if (arrayDimensions > 0)
            b.append("_array").append(arrayDimensions);
        b.append("__");
        if (primitiveType == null)
            b.append(type);
        else
            b.append(Util.getCType(primitiveType));
    }
    
    public List<String> getDependentClasses() {
        return dependentClasses;
    }
    
    //public List<String> getExportedClasses() {
    //    return exportedClasses;
    //}
    /**
     * @return the staticField
     */
    public boolean isStaticField() {
        return staticField;
    }
    
    @Override
    public boolean equals(Object o) {
        return fieldName.equals(((ByteCodeField)o).fieldName);
    }
    
    @Override
    public int hashCode() {
        return fieldName.hashCode();
    }

    /**
     * @return the clsName
     */
    public String getClsName() {
        return clsName;
    }
    
    public boolean isObjectType() {
        return arrayDimensions > 0 || primitiveType == null;
    }
    
    public boolean isFinal() {
        return finalField;
    }
    
    public boolean shouldRemoveFromHeapCollection() {
        if(finalField && isObjectType()) {
            // 2d arrays can be modified in runtime resulting in broken arrays
            if(arrayDimensions < 2) {
                // arrays of non-primitive types are mutable despite being marked as final
                if(type != null && arrayDimensions > 0) {
                    return false;
                }
                if(type == null || type.startsWith("java_lang_") && !type.endsWith("Builder") && !type.endsWith("Buffer")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public Object getValue() {
        return value;
    }
    
    public String getType() {
        return type;
    }
    
    public boolean isPrivate() {
        return privateField;
    }
}
