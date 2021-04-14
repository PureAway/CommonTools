/*
 * Copyright 2020 ObjectBox Ltd. All rights reserved.
 *
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

// automatically generated by the FlatBuffers compiler, do not modify

package io.objectbox.model;

import java.nio.*;
import java.lang.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
/**
 * ID tuple: besides the main ID there is also a UID for verification
 */
public final class IdUid extends Struct {
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public IdUid __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public long id() { return (long)bb.getInt(bb_pos + 0) & 0xFFFFFFFFL; }
  /**
   * Unique ID (within the model) used to verify external managed IDs.
   * UIDs are also used within the model to make stable references (IDs might conflict during code merges).
   */
  public long uid() { return bb.getLong(bb_pos + 8); }

  public static int createIdUid(FlatBufferBuilder builder, long id, long uid) {
    builder.prep(8, 16);
    builder.putLong(uid);
    builder.pad(4);
    builder.putInt((int)id);
    return builder.offset();
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public IdUid get(int j) { return get(new IdUid(), j); }
    public IdUid get(IdUid obj, int j) {  return obj.__assign(__element(j), bb); }
  }
}
