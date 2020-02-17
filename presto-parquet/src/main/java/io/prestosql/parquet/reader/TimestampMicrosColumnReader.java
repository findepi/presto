/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.parquet.reader;

import io.prestosql.parquet.RichColumnDescriptor;
import io.prestosql.spi.block.BlockBuilder;
import io.prestosql.spi.type.LongTimestamp;
import io.prestosql.spi.type.LongTimestampWithTimeZone;
import io.prestosql.spi.type.Timestamps;
import io.prestosql.spi.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static io.prestosql.spi.type.DateTimeEncoding.packDateTimeWithZone;
import static io.prestosql.spi.type.TimeZoneKey.UTC_KEY;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP_MICROS;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP_MILLIS;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP_NANOS;
import static io.prestosql.spi.type.TimestampWithTimeZoneType.TIMESTAMP_TZ_MICROS;
import static io.prestosql.spi.type.TimestampWithTimeZoneType.TIMESTAMP_TZ_MILLIS;
import static io.prestosql.spi.type.TimestampWithTimeZoneType.TIMESTAMP_TZ_NANOS;
import static io.prestosql.spi.type.Timestamps.MICROSECONDS_PER_MILLISECOND;
import static io.prestosql.spi.type.Timestamps.PICOSECONDS_PER_MICROSECOND;
import static java.lang.Math.floorDiv;
import static java.lang.Math.toIntExact;

public class TimestampMicrosColumnReader
        extends PrimitiveColumnReader
{
    public TimestampMicrosColumnReader(RichColumnDescriptor descriptor, Type prestoType)
    {
        super(descriptor, prestoType);
        checkArgument(
                prestoType == TIMESTAMP_MILLIS ||
                        prestoType == TIMESTAMP_MICROS ||
                        prestoType == TIMESTAMP_NANOS ||
                        prestoType == TIMESTAMP_TZ_MILLIS ||
                        prestoType == TIMESTAMP_TZ_MICROS ||
                        prestoType == TIMESTAMP_TZ_NANOS,
                "Unsupported type: %s",
                prestoType);
    }

    @Override
    protected void readValue(BlockBuilder blockBuilder)
    {
        if (definitionLevel == columnDescriptor.getMaxDefinitionLevel()) {
            long epochMicros = valuesReader.readLong();
            // TODO: specialize the class at creation time
            if (prestoType == TIMESTAMP_MILLIS) {
                prestoType.writeLong(blockBuilder, Timestamps.round(epochMicros, 3));
            }
            else if (prestoType == TIMESTAMP_MICROS) {
                prestoType.writeLong(blockBuilder, epochMicros);
            }
            else if (prestoType == TIMESTAMP_NANOS) {
                prestoType.writeObject(blockBuilder, new LongTimestamp(epochMicros, 0));
            }
            else if (prestoType == TIMESTAMP_TZ_MILLIS) {
                long epochMillis = Timestamps.round(epochMicros, 3) / MICROSECONDS_PER_MILLISECOND;
                prestoType.writeLong(blockBuilder, packDateTimeWithZone(epochMillis, UTC_KEY));
            }
            else if (prestoType == TIMESTAMP_TZ_MICROS || prestoType == TIMESTAMP_TZ_NANOS) {
                long epochMillis = floorDiv(epochMicros, MICROSECONDS_PER_MILLISECOND);
                int picosOfMillis = toIntExact(epochMicros % MICROSECONDS_PER_MILLISECOND) * PICOSECONDS_PER_MICROSECOND;
                prestoType.writeObject(blockBuilder, LongTimestampWithTimeZone.fromEpochMillisAndFraction(epochMillis, picosOfMillis, UTC_KEY));
            }
            else {
                throw new IllegalArgumentException("wrong type: " + prestoType);
            }
        }
        else if (isValueNull()) {
            blockBuilder.appendNull();
        }
    }

    @Override
    protected void skipValue()
    {
        if (definitionLevel == columnDescriptor.getMaxDefinitionLevel()) {
            valuesReader.readLong();
        }
    }
}
