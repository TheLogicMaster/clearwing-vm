/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.text;

import java.util.Comparator;
import java.util.Locale;

public abstract class Collator implements Comparator<Object> {
    /**
     * Constant used to specify the decomposition rule.
     */
    public static final int NO_DECOMPOSITION = 0;

    /**
     * Constant used to specify the decomposition rule.
     */
    public static final int CANONICAL_DECOMPOSITION = 1;

    /**
     * Constant used to specify the decomposition rule. This value for
     * decomposition is not supported.
     */
    public static final int FULL_DECOMPOSITION = 2;

    /**
     * Constant used to specify the collation strength.
     */
    public static final int PRIMARY = 0;

    /**
     * Constant used to specify the collation strength.
     */
    public static final int SECONDARY = 1;

    /**
     * Constant used to specify the collation strength.
     */
    public static final int TERTIARY = 2;

    /**
     * Constant used to specify the collation strength.
     */
    public static final int IDENTICAL = 3;

    /**
     * Constructs a new {@code Collator} instance.
     */
    protected Collator() {
    }
    
    /**
     * Compares two objects to determine their relative order. The objects must
     * be strings.
     *
     * @param object1
     *            the first string to compare.
     * @param object2
     *            the second string to compare.
     * @return a negative value if {@code object1} is less than {@code object2},
     *         0 if they are equal, and a positive value if {@code object1} is
     *         greater than {@code object2}.
     * @throws ClassCastException
     *         if {@code object1} or {@code object2} is not a {@code String}.
     */
    public int compare(Object object1, Object object2) {
        return compare((String) object1, (String) object2);
    }

    /**
     * Compares two strings to determine their relative order.
     *
     * @param string1
     *            the first string to compare.
     * @param string2
     *            the second string to compare.
     * @return a negative value if {@code string1} is less than {@code string2},
     *         0 if they are equal and a positive value if {@code string1} is
     *         greater than {@code string2}.
     */
    public abstract int compare(String string1, String string2);

    /**
     * Compares two strings using the collation rules to determine if they are
     * equal.
     *
     * @param string1
     *            the first string to compare.
     * @param string2
     *            the second string to compare.
     * @return {@code true} if {@code string1} and {@code string2} are equal
     *         using the collation rules, false otherwise.
     */
    public boolean equals(String string1, String string2) {
        return compare(string1, string2) == 0;
    }

    /**
     * Returns an array of locales for which custom {@code Collator} instances
     * are available.
     * <p>Note that Android does not support user-supplied locale service providers.
     */
    public static Locale[] getAvailableLocales() {
        return new Locale[0];
    }

    /**
     * Returns a {@link CollationKey} for the specified string for this collator
     * with the current decomposition rule and strength value.
     *
     * @param string
     *            the source string that is converted into a collation key.
     * @return the collation key for {@code string}.
     */
    public abstract CollationKey getCollationKey(String string);

    /**
     * Returns the decomposition rule for this collator.
     *
     * @return the decomposition rule, either {@code NO_DECOMPOSITION} or
     *         {@code CANONICAL_DECOMPOSITION}. {@code FULL_DECOMPOSITION} is
     *         not supported.
     */
    public int getDecomposition() {
        return NO_DECOMPOSITION;
    }

    /**
     * Returns a {@code Collator} instance which is appropriate for the user's default
     * {@code Locale}.
     * See "<a href="../util/Locale.html#default_locale">Be wary of the default locale</a>".
     */
    public static Collator getInstance() {
        return getInstance(Locale.getDefault());
    }

    /**
     * Returns a {@code Collator} instance which is appropriate for {@code locale}.
     */
    public static Collator getInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return null;
    }

    /**
     * Returns the strength value for this collator.
     *
     * @return the strength value, either PRIMARY, SECONDARY, TERTIARY or
     *         IDENTICAL.
     */
    public int getStrength() {
        return IDENTICAL;
    }

    @Override
    public abstract int hashCode();

    /**
     * Sets the decomposition rule for this collator.
     *
     * @param value
     *            the decomposition rule, either {@code NO_DECOMPOSITION} or
     *            {@code CANONICAL_DECOMPOSITION}. {@code FULL_DECOMPOSITION}
     *            is not supported.
     * @throws IllegalArgumentException
     *            if the provided decomposition rule is not valid. This includes
     *            {@code FULL_DECOMPOSITION}.
     */
    public void setDecomposition(int value) {
    }

    /**
     * Sets the strength value for this collator.
     *
     * @param value
     *            the strength value, either PRIMARY, SECONDARY, TERTIARY, or
     *            IDENTICAL.
     * @throws IllegalArgumentException
     *            if the provided strength value is not valid.
     */
    public void setStrength(int value) {
    }
}
