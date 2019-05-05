/*
 * Copyright 2015-2019 Alexandr Evstigneev
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

package com.perl5.lang.perl.psi.mixins;

import com.intellij.lang.ASTNode;
import com.perl5.lang.perl.psi.PerlAnnotationInject;
import com.perl5.lang.perl.psi.PsiPerlStringBare;
import com.perl5.lang.perl.psi.impl.PerlCompositeElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class PerlAnnotationInjectMixin extends PerlCompositeElementImpl implements PerlAnnotationInject {
  public PerlAnnotationInjectMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Nullable
  @Override
  public String getLanguageMarker() {
    PsiPerlStringBare stringBare = getStringBare();
    return stringBare == null ? null : stringBare.getText();
  }
}
