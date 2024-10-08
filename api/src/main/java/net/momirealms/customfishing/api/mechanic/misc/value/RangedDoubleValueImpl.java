/*
 *  Copyright (C) <2024> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing.api.mechanic.misc.value;

import net.momirealms.customfishing.api.mechanic.context.Context;
import net.momirealms.customfishing.common.util.RandomUtils;

public class RangedDoubleValueImpl<T> implements MathValue<T> {

    private final MathValue<T> min;
    private final MathValue<T> max;

    public RangedDoubleValueImpl(String value) {
        String[] split = value.split("~");
        if (split.length != 2) {
            throw new IllegalArgumentException("Correct ranged format `a~b`");
        }
        this.min = MathValue.auto(split[0]);
        this.max = MathValue.auto(split[1]);
    }

    @Override
    public double evaluate(Context<T> context) {
        return RandomUtils.generateRandomDouble(min.evaluate(context), max.evaluate(context));
    }
}
