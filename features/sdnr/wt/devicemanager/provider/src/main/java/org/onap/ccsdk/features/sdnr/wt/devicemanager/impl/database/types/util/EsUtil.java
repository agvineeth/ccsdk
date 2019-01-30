/*******************************************************************************
 * ============LICENSE_START========================================================================
 * ONAP : ccsdk feature sdnr wt
 * =================================================================================================
 * Copyright (C) 2019 highstreet technologies GmbH Intellectual Property. All rights reserved.
 * =================================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * ============LICENSE_END==========================================================================
 ******************************************************************************/
package org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.database.types.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsUtil {

    private static final Logger LOG = LoggerFactory.getLogger(EsUtil.class);

    static private List<String> SKIP = Arrays.asList("LOG", "rawHeaders");

    /**
     * Create a list of objects
     *
     * @param listOfK
     * @param clazzT
     * @return
     */
    @SuppressWarnings("unchecked")
    static <T, K> List<T> getList(Class<T> clazzT, List<K> listOfK) {

        List<T> listOfT = new ArrayList<>();
        if (listOfK != null && !listOfK.isEmpty()) {
            K obkK = listOfK.get(0);
            Constructor<?> c;
            try {
                c = Class.forName(clazzT.getCanonicalName()).getConstructor(obkK.getClass());
                for (K objK : listOfK) {
                    listOfT.add((T) c.newInstance(objK));
                }
            } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException
                    | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }
        return listOfT;
    }

    static public <T> String inspect(Class<T> klazz) {
        StringBuffer buf = new StringBuffer();
        Field[] fields = klazz.getDeclaredFields();
        buf.append(fields.length);
        buf.append(" fields: ");
        for (Field field : fields) {
            buf.append("[");
            buf.append(Modifier.toString(field.getModifiers()));
            buf.append(" ");
            buf.append(field.getType().getCanonicalName());
            buf.append(" ");
            buf.append(field.getName());
            buf.append("]");


        }
        return buf.toString();
    }

    static public String inspect(Object obj, int n) {

        StringBuffer buf = new StringBuffer();
        if (n < 7) {

            Class<?> klazz = obj.getClass();

            Field[] fields = klazz.getDeclaredFields();
            buf.append("\n");
            // buf.append(fields.length);
            // buf.append(" fields: ");
            int fieldNo = 0;
            for (Field field : fields) {
                fieldNo++;
                buf.append("\n%L" + n + "(" + fieldNo + ")");
                for (int t = 0; t < n; t++) {
                    buf.append("  ");
                }
                buf.append("[");
                buf.append(Modifier.toString(field.getModifiers()));
                buf.append(" ");
                buf.append(field.getType().getCanonicalName());
                buf.append(" ");
                buf.append(field.getName());

                Object subObj = null;
                try {
                    field.setAccessible(true);
                    subObj = field.get(obj);
                    if (subObj == null) {
                        buf.append(" xnull");
                    } else {
                        // buf.append(" fieldclass "+field.getType()+ " subClass "+subObj.getClass());

                        if (field.getType().isEnum()) {
                            buf.append(" leaf1:" + subObj.toString());
                        } else if (field.getType().isPrimitive()) {
                            buf.append(" leaf2:" + subObj.toString());
                        } else if (field.getType().isArray()) {
                            buf.append(" leaf3:" + subObj.toString());
                        } else if (field.getType() == List.class) {
                            buf.append("[");
                            if (subObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Object> subObjList = (List<Object>) subObj;
                                int t1 = 0;
                                for (Object subObjListElement : subObjList) {
                                    if (t1++ > 0) {
                                        buf.append(", ");
                                    }
                                    inspect(subObjListElement, n + 1);
                                }
                                buf.append("]");
                            } else {
                                buf.append(" leaf4.1: {}" + subObj.getClass().getCanonicalName());
                            }
                        } else if (field.getType() == Method.class) {
                            buf.append(" leaf5:" + subObj.toString());
                            if (subObj instanceof Method) {
                                Method method = (Method) subObj;
                                Object result = method.invoke(obj);
                                if (result != null) {
                                    buf.append(" leaf5.1:" + result.getClass().getName());
                                    inspect(result, n + 1);
                                } else {
                                    buf.append(" leaf5.1:null");
                                }
                            }
                        } else if (field.getType() == Map.class) {
                            buf.append(" leaf6: [");
                            if (subObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<Object, Object> map = (Map<Object, Object>) subObj;
                                Iterator<Entry<Object, Object>> it = map.entrySet().iterator();
                                while (it.hasNext()) {
                                    Entry<Object, Object> pair = it.next();
                                    buf.append(pair.getKey() + " = " + pair.getValue() + ", ");
                                    // it.remove(); // avoids a ConcurrentModificationException
                                }
                                buf.append("]");
                            } else {
                                buf.append(" leaf7: wrong type");
                            }
                        } else if (field.getType() == subObj.getClass()) {
                            buf.append(" leaf8:" + subObj.toString());
                        } else {
                            buf.append(" Dive: ");
                            buf.append(inspect(subObj, n + 1));
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                buf.append("]");
            }
        } else {
            buf.append(obj.toString());
            buf.append(" - ENDE ");
        }
        return buf.toString();
    }

    static public String toJson(Object obj) {
        return toJson(obj, 0);
    }

    static private String toJson(Object obj, int n) {

        StringBuffer buf = new StringBuffer();
        if (n < 10) {

            Class<?> klazz = obj.getClass();

            Field[] fields = klazz.getDeclaredFields();
            int mapIdx = 0;
            buf.append("{");
            // buf.append(fields.length);
            // buf.append(" fields: ");
            int t = 0;
            for (Field field : fields) {

                if (SKIP.contains(field.getName())) {
                    LOG.info("Skip " + field.getName());
                    continue;
                }

                if (t++ > 0) {
                    buf.append(",");
                }

                buf.append("\"");
                buf.append(field.getName());
                buf.append("\":");

                Object subObj = null;
                try {
                    field.setAccessible(true);
                    subObj = field.get(obj);
                    if (subObj == null) {
                        buf.append("null");
                    } else {
                        // buf.append(" fieldclass "+field.getType()+ " subClass "+subObj.getClass());

                        if (field.getType().isEnum()) {
                            buf.append("\"");
                            buf.append(subObj.toString());
                            buf.append("\"");
                        } else if (field.getType().isPrimitive()) {
                            buf.append("\"");
                            buf.append(subObj.toString());
                            buf.append("\"");
                        } else if (field.getType().isArray()) {
                            LOG.info("leaf3 type" + field.getType() + " String: " + subObj.toString());
                            buf.append("\"leaf3\"");
                        } else if (field.getType() == List.class) {
                            buf.append("[");
                            if (subObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Object> subObjList = (List<Object>) subObj;
                                int t1 = 0;
                                for (Object subObjListElement : subObjList) {
                                    if (t1++ > 0) {
                                        buf.append(", ");
                                    }
                                    inspect(subObjListElement, n + 1);
                                }
                                buf.append("]");
                            } else {
                                buf.append(" leaf4.1: {}" + subObj.getClass().getCanonicalName());
                            }

                        } else if (field.getType() == Map.class) {
                            if (subObj instanceof Map) {

                                buf.append("\"MAP" + mapIdx++ + "[");
                                String strVar;
                                @SuppressWarnings("unchecked")
                                Map<Object, Object> map = (Map<Object, Object>) subObj;
                                Iterator<Entry<Object, Object>> it = map.entrySet().iterator();
                                while (it.hasNext()) {
                                    Entry<Object, Object> pair = it.next();
                                    strVar = pair.getKey() + " = " + pair.getValue() + ", ";
                                    strVar = strVar.replace("\"", "'");
                                    buf.append(strVar);
                                    // it.remove(); // avoids a ConcurrentModificationException
                                }
                                buf.append("]\"");
                            } else {
                                LOG.info("leaf5 type" + field.getType() + " String: " + subObj.toString());
                                buf.append("\"leaf5\"");
                            }
                        } else if (field.getType() == subObj.getClass()) {
                            buf.append("\"");
                            buf.append(subObj.toString());
                            buf.append("\"");
                        } else {
                            buf.append(toJson(subObj, n + 1));
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            buf.append("}");
        } else {
            LOG.info("leaf7 ENDE");
        }
        return buf.toString();
    }

}
