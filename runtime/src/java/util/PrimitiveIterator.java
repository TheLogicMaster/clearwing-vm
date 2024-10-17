package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public interface PrimitiveIterator<T, T_CONS> extends Iterator<T> {
    void forEachRemaining(T_CONS var1);

    public interface OfDouble extends PrimitiveIterator<Double, DoubleConsumer> {
        double nextDouble();

        default void forEachRemaining(DoubleConsumer action) {
            Objects.requireNonNull(action);

            while(this.hasNext()) {
                action.accept(this.nextDouble());
            }

        }

        default Double next() {
            if (Tripwire.ENABLED) {
                Tripwire.trip(this.getClass(), "{0} calling PrimitiveIterator.OfDouble.nextLong()");
            }

            return this.nextDouble();
        }

        default void forEachRemaining(Consumer<? super Double> action) {
            if (action instanceof DoubleConsumer) {
                this.forEachRemaining((DoubleConsumer)action);
            } else {
                Objects.requireNonNull(action);
                if (Tripwire.ENABLED) {
                    Tripwire.trip(this.getClass(), "{0} calling PrimitiveIterator.OfDouble.forEachRemainingDouble(action::accept)");
                }

                Objects.requireNonNull(action);
                this.forEachRemaining((DoubleConsumer) action::accept);
            }

        }
    }

    public interface OfLong extends PrimitiveIterator<Long, LongConsumer> {
        long nextLong();

        default void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action);

            while(this.hasNext()) {
                action.accept(this.nextLong());
            }

        }

        default Long next() {
            if (Tripwire.ENABLED) {
                Tripwire.trip(this.getClass(), "{0} calling PrimitiveIterator.OfLong.nextLong()");
            }

            return this.nextLong();
        }

        default void forEachRemaining(Consumer<? super Long> action) {
            if (action instanceof LongConsumer) {
                this.forEachRemaining((LongConsumer)action);
            } else {
                Objects.requireNonNull(action);
                if (Tripwire.ENABLED) {
                    Tripwire.trip(this.getClass(), "{0} calling PrimitiveIterator.OfLong.forEachRemainingLong(action::accept)");
                }

                Objects.requireNonNull(action);
                this.forEachRemaining((LongConsumer) action::accept);
            }

        }
    }

    public interface OfInt extends PrimitiveIterator<Integer, IntConsumer> {
        int nextInt();

        default void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action);

            while(this.hasNext()) {
                action.accept(this.nextInt());
            }

        }

        default Integer next() {
            if (Tripwire.ENABLED) {
                Tripwire.trip(this.getClass(), "{0} calling PrimitiveIterator.OfInt.nextInt()");
            }

            return this.nextInt();
        }

        default void forEachRemaining(Consumer<? super Integer> action) {
            if (action instanceof IntConsumer) {
                this.forEachRemaining((IntConsumer)action);
            } else {
                Objects.requireNonNull(action);
                if (Tripwire.ENABLED) {
                    Tripwire.trip(this.getClass(), "{0} calling PrimitiveIterator.OfInt.forEachRemainingInt(action::accept)");
                }

                Objects.requireNonNull(action);
                this.forEachRemaining((IntConsumer) action::accept);
            }

        }
    }
}
