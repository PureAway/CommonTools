package io.objectbox;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;

import java.util.HashMap;

// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * ObjectBox generated Cursor implementation for "RequestHistory".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class RequestHistoryCursor extends Cursor<RequestHistory> {
    @Internal
    static final class Factory implements CursorFactory<RequestHistory> {
        @Override
        public Cursor<RequestHistory> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new RequestHistoryCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final RequestHistory_.RequestHistoryIdGetter ID_GETTER = RequestHistory_.__ID_GETTER;

    private final RequestHistory.MapConverter headersConverter = new RequestHistory.MapConverter();
    private final RequestHistory.MapConverter queryMapsConverter = new RequestHistory.MapConverter();

    private final static int __ID_method = RequestHistory_.method.id;
    private final static int __ID_url = RequestHistory_.url.id;
    private final static int __ID_headers = RequestHistory_.headers.id;
    private final static int __ID_queryMaps = RequestHistory_.queryMaps.id;
    private final static int __ID_bodyJson = RequestHistory_.bodyJson.id;
    private final static int __ID_date = RequestHistory_.date.id;

    public RequestHistoryCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, RequestHistory_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(RequestHistory entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(RequestHistory entity) {
        String method = entity.method;
        int __id1 = method != null ? __ID_method : 0;
        String url = entity.url;
        int __id2 = url != null ? __ID_url : 0;
        HashMap headers = entity.headers;
        int __id3 = headers != null ? __ID_headers : 0;
        HashMap queryMaps = entity.queryMaps;
        int __id4 = queryMaps != null ? __ID_queryMaps : 0;

        collect400000(cursor, 0, PUT_FLAG_FIRST,
                __id1, method, __id2, url,
                __id3, __id3 != 0 ? headersConverter.convertToDatabaseValue(headers) : null, __id4, __id4 != 0 ? queryMapsConverter.convertToDatabaseValue(queryMaps) : null);

        String bodyJson = entity.bodyJson;
        int __id5 = bodyJson != null ? __ID_bodyJson : 0;
        java.util.Date date = entity.date;
        int __id6 = date != null ? __ID_date : 0;

        long __assignedId = collect313311(cursor, entity.id, PUT_FLAG_COMPLETE,
                __id5, bodyJson, 0, null,
                0, null, 0, null,
                __id6, __id6 != 0 ? date.getTime() : 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;

        return __assignedId;
    }

}
