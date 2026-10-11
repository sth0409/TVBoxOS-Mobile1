package com.kukuqi.tvbox.osc.util;

import com.kukuqi.tvbox.osc.bean.Subscription;
import org.junit.After;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class AdultSourcesTest {
    @After public void resetVisibility() { AdultSources.setVisible(false); }

    @Test public void onlyBundledAdultSubscriptionsAreHidden() {
        List<Subscription> items = Arrays.asList(
                new Subscription("普通", SourceDefaults.VOD),
                new Subscription("R18", AdultSources.R18),
                new Subscription("18cn", AdultSources.CN18),
                new Subscription("自行导入", "https://example.com/18.json"));

        AdultSources.setVisible(false);
        assertEquals(2, AdultSources.visibleItems(items).size());
        assertEquals("自行导入", AdultSources.visibleItems(items).get(1).getName());
        AdultSources.setVisible(true);
        assertEquals(4, AdultSources.visibleItems(items).size());
    }

    @Test public void hidingSelectedAdultSourceChoosesNormalDefault() {
        List<Subscription> items = new ArrayList<>(Arrays.asList(
                new Subscription("18cn", AdultSources.CN18).setChecked(true),
                new Subscription("普通", SourceDefaults.VOD)));

        AdultSources.setVisible(false);
        assertEquals(SourceDefaults.VOD, AdultSources.safeSelection(items, AdultSources.CN18));
        assertFalse(items.get(0).isChecked());
        assertTrue(items.get(1).isChecked());
    }
}
