/*
 * Copyright 2015-2018 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PerlTypeArray extends PerlTypeWrapping {
  public PerlTypeArray(@NotNull PerlType innerType) {
    super(innerType);
  }

  public static PerlTypeArray fromInnerType(@Nullable PerlType innerType) {
    if (innerType == null) {
      return null;
    }
    return new PerlTypeArray(innerType);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PerlTypeArray && super.equals(o);
  }

  @Override
  public String toString() {
    return ARRAY + super.toString();
  }
}
