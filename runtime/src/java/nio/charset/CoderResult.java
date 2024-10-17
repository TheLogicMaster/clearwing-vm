/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.nio.charset;

public class CoderResult {
    public static final CoderResult OVERFLOW = null;
    public static final CoderResult UNDERFLOW = null;

    private CoderResult() {
        throw new RuntimeException("Stub!");
    }

    public String toString() {
        throw new RuntimeException("Stub!");
    }

    public boolean isUnderflow() {
        throw new RuntimeException("Stub!");
    }

    public boolean isOverflow() {
        throw new RuntimeException("Stub!");
    }

    public boolean isError() {
        throw new RuntimeException("Stub!");
    }

    public boolean isMalformed() {
        throw new RuntimeException("Stub!");
    }

    public boolean isUnmappable() {
        throw new RuntimeException("Stub!");
    }

    public int length() {
        throw new RuntimeException("Stub!");
    }

    public static CoderResult malformedForLength(int length) {
        throw new RuntimeException("Stub!");
    }

    public static CoderResult unmappableForLength(int length) {
        throw new RuntimeException("Stub!");
    }

    public void throwException() throws CharacterCodingException {
        throw new RuntimeException("Stub!");
    }
}
