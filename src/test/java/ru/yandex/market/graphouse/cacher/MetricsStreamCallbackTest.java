package ru.yandex.market.graphouse.cacher;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.clickhouse.settings.ClickHouseProperties;
import ru.yandex.clickhouse.util.ClickHouseRowBinaryStream;
import ru.yandex.market.graphouse.Metric;
import ru.yandex.market.graphouse.retention.DefaultRetentionProvider;
import ru.yandex.market.graphouse.search.MetricStatus;
import ru.yandex.market.graphouse.search.tree.InMemoryMetricDir;
import ru.yandex.market.graphouse.search.tree.MetricDir;
import ru.yandex.market.graphouse.search.tree.MetricName;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.TimeZone;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 16/04/2017
 */
public class MetricsStreamCallbackTest {


    @Test
    public void testTimeZones() {
        MetricsStreamCallback entity = new MetricsStreamCallback(null, TimeZone.getTimeZone("Europe/Moscow"));
        Assert.assertEquals(17265, entity.getUnsignedDaysSinceEpoch(1491771599));
        Assert.assertEquals(17266, entity.getUnsignedDaysSinceEpoch(1491771601));
    }

    @Test
    public void testTimeZones2() {
        LocalDate localDate = LocalDate.of(2017, 4, 9);
        MetricsStreamCallback entity = new MetricsStreamCallback(null, TimeZone.getTimeZone("Europe/Moscow").toZoneId(), localDate);

        Assert.assertEquals(17265, entity.getCurrentDay());
        Assert.assertEquals(1491685200, entity.getTodayStartSeconds());
        Assert.assertEquals(1491771600, entity.getTodayEndSeconds());

    }


    @Test
    public void writeMetric() throws Exception {

        MetricDir root = new InMemoryMetricDir(null, null, MetricStatus.SIMPLE);
        MetricDir a = new InMemoryMetricDir(root, "a", MetricStatus.SIMPLE);
        MetricDir b = new InMemoryMetricDir(a, "b", MetricStatus.SIMPLE);
        MetricName c = new MetricName(b, "c", MetricStatus.SIMPLE, new DefaultRetentionProvider());

        Metric metric = new Metric(
            c,
            1492342562,
            42.21,
            1492350000
        );

        MetricsStreamCallback entity = new MetricsStreamCallback(Collections.singletonList(metric), TimeZone.getTimeZone("Europe/Moscow"));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        entity.writeTo(new ClickHouseRowBinaryStream(byteArrayOutputStream, null, new ClickHouseProperties()));

        byte[] actual = byteArrayOutputStream.toByteArray();

        //Result of
        //clickhouse-client -q "select 'a.b.c', toFloat64(42.21), toUInt32(1492342562), toDate('2017-04-16'), toUInt32(1492350000) format RowBinary" | od -vAn -td1
        byte[] expected = {
            5, 97, 46, 98, 46, 99, 123, 20, -82, 71, -31, 26, 69, 64, 34, 87, -13, 88, 120, 67, 48, 116, -13, 88
        };
        Assert.assertArrayEquals(expected, actual);
    }

}