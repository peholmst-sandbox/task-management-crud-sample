package com.example.application.base.ui.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import org.jspecify.annotations.Nullable;

import java.time.DateTimeException;
import java.time.ZoneId;

public final class CurrentTimezone {

    private CurrentTimezone() {
    }

    public static void get(SerializableConsumer<ZoneId> receiver) {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(
                extendedClientDetails -> receiver.accept(parseZoneInfo(extendedClientDetails.getTimeZoneId())));
    }

    private static ZoneId parseZoneInfo(@Nullable String zoneInfo) {
        if (zoneInfo == null) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(zoneInfo);
        } catch (DateTimeException e) {
            return ZoneId.systemDefault();
        }
    }
}
