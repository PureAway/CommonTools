package com.haier.uhome.plugins.model;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.converter.PropertyConverter;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
public class RequestHistory {

    @Id
    public long id;
    public String method;
    public String url;
    @Convert(converter = MapConverter.class, dbType = String.class)
    public HashMap<String, String> headers;
    @Convert(converter = MapConverter.class, dbType = String.class)
    public HashMap<String, String> queryMaps;
    public String bodyJson;
    public Date date;

    public RequestHistory(long id,
                          String method,
                          String url,
                          HashMap<String, String> headers,
                          HashMap<String, String> queryMaps,
                          String bodyJson, Date date) {
        this.id = id;
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.queryMaps = queryMaps;
        this.bodyJson = bodyJson;
        this.date = date;
    }

    public RequestHistory() {
    }

    @Override
    public String toString() {
        return "RequestHistory{" +
                "id=" + id +
                ", method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", headers=" + headers +
                ", queryMaps=" + queryMaps +
                ", bodyJson='" + bodyJson + '\'' +
                ", date=" + date +
                '}';
    }

    public static class MapConverter implements PropertyConverter<Map<String, String>, String> {

        @Override
        public Map<String, String> convertToEntityProperty(String databaseValue) {
            if (databaseValue == null) {
                return null;
            }
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            return new GsonBuilder().create().fromJson(databaseValue, type);
        }

        @Override
        public String convertToDatabaseValue(Map<String, String> entityProperty) {
            return entityProperty == null ? null : new GsonBuilder().create().toJson(entityProperty);
        }
    }
}
