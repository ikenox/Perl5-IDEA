/*
 * Copyright 2015-2017 Alexandr Evstigneev
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

package com.perl5.lang.perl.psi;

import com.intellij.openapi.util.text.StringUtil;
import com.perl5.lang.perl.psi.utils.PerlVariableAnnotations;
import com.perl5.lang.perl.psi.utils.PerlVariableType;
import com.perl5.lang.perl.types.PerlType;
import org.jetbrains.annotations.Nullable;

import static com.perl5.lang.perl.util.PerlPackageUtil.PACKAGE_SEPARATOR;

public interface PerlVariableDeclaration extends PerlDeprecatable {
  /**
   * Trying to get the package name from explicit specification or by traversing
   *
   * @return package name for current element
   */
  String getPackageName();

  String getVariableName();

  /**
   * Returns declaration type in annotation or declaration
   *
   * @return type string or null
   */
  @Nullable
  PerlType getDeclaredType();

  /**
   * Guessing actual variable type from context
   *
   * @return variable type
   */
  PerlVariableType getActualType();

  /**
   * Returns stubbed, local or external variable annotations
   *
   * @return annotations or null
   */
  @Nullable
  PerlVariableAnnotations getVariableAnnotations();

  @Override
  default boolean isDeprecated() {
    PerlVariableAnnotations variableAnnotations = getVariableAnnotations();
    return variableAnnotations != null && variableAnnotations.isDeprecated();
  }

  /**
   * returns proper fqn
   *
   * @return fqn or null if name is missing
   */
  @Nullable
  default String getFullQualifiedName() {
    String variableName = getVariableName();
    if (StringUtil.isEmpty(variableName)) {
      return null;
    }

    String packageName = getPackageName();
    if (StringUtil.isEmpty(packageName)) {
      return variableName;
    }
    return packageName + PACKAGE_SEPARATOR + variableName;
  }
}
