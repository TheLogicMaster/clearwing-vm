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

package java.util;

public final class StringJoiner {
    private final String prefix;
    private final String delimiter;
    private final String suffix;
    private String[] elts;
    private int size;
    private int len;
    private String emptyValue;

    public StringJoiner(CharSequence delimiter) {
        this(delimiter, "", "");
    }

    public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        Objects.requireNonNull(prefix, "The prefix must not be null");
        Objects.requireNonNull(delimiter, "The delimiter must not be null");
        Objects.requireNonNull(suffix, "The suffix must not be null");
        this.prefix = prefix.toString();
        this.delimiter = delimiter.toString();
        this.suffix = suffix.toString();
    }

    public StringJoiner setEmptyValue(CharSequence emptyValue) {
        this.emptyValue = ((CharSequence)Objects.requireNonNull(emptyValue, "The empty value must not be null")).toString();
        return this;
    }

    private static int getChars(String s, char[] chars, int start) {
        int len = s.length();
        s.getChars(0, len, chars, start);
        return len;
    }

    public String toString() {
        String[] elts = this.elts;
        if (elts == null && this.emptyValue != null) {
            return this.emptyValue;
        } else {
            int size = this.size;
            int addLen = this.prefix.length() + this.suffix.length();
            if (addLen == 0) {
                this.compactElts();
                return size == 0 ? "" : elts[0];
            } else {
                String delimiter = this.delimiter;
                char[] chars = new char[this.len + addLen];
                int k = getChars(this.prefix, chars, 0);
                if (size > 0) {
                    k += getChars(elts[0], chars, k);

                    for(int i = 1; i < size; ++i) {
                        k += getChars(delimiter, chars, k);
                        k += getChars(elts[i], chars, k);
                    }
                }

                k += getChars(this.suffix, chars, k);
                return new String(chars);
            }
        }
    }

    public StringJoiner add(CharSequence newElement) {
        String elt = String.valueOf(newElement);
        if (this.elts == null) {
            this.elts = new String[8];
        } else {
            if (this.size == this.elts.length) {
                this.elts = (String[])Arrays.copyOf(this.elts, 2 * this.size);
            }

            this.len += this.delimiter.length();
        }

        this.len += elt.length();
        this.elts[this.size++] = elt;
        return this;
    }

    public StringJoiner merge(StringJoiner other) {
        Objects.requireNonNull(other);
        if (other.elts == null) {
            return this;
        } else {
            other.compactElts();
            return this.add(other.elts[0]);
        }
    }

    private void compactElts() {
        if (this.size > 1) {
            char[] chars = new char[this.len];
            int i = 1;
            int k = getChars(this.elts[0], chars, 0);

            do {
                k += getChars(this.delimiter, chars, k);
                k += getChars(this.elts[i], chars, k);
                this.elts[i] = null;
                ++i;
            } while(i < this.size);

            this.size = 1;
            this.elts[0] = new String(chars);
        }

    }

    public int length() {
        return this.size == 0 && this.emptyValue != null ? this.emptyValue.length() : this.len + this.prefix.length() + this.suffix.length();
    }
}

