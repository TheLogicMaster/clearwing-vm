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

package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Predicate<T> {
    boolean test(T var1);

    default Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> {
            return this.test(t) && other.test(t);
        };
    }

    default Predicate<T> negate() {
        return (t) -> {
            return !this.test(t);
        };
    }

    default Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> {
            return this.test(t) || other.test(t);
        };
    }

    static <T> Predicate<T> isEqual(Object targetRef) {
        return null == targetRef ? Objects::isNull : (object) -> {
            return targetRef.equals(object);
        };
    }

    static <T> Predicate<T> not(Predicate<T> target) {
        Objects.requireNonNull(target);
        return target.negate();
    }
}
