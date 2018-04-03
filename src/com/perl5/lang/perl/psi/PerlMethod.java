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

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.perl5.lang.perl.psi.properties.PerlNamespaceElementContainer;
import com.perl5.lang.perl.psi.properties.PerlPackageMember;
import com.perl5.lang.perl.psi.properties.PerlSubNameElementContainer;
import com.perl5.lang.perl.psi.utils.PerlResolveUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Created by hurricup on 31.05.2015.
 * Invocable method class
 */
public interface PerlMethod extends PerlNamespaceElementContainer, PerlSubNameElementContainer, PerlPackageMember {
  /**
   * Checks if explicit namespace defined - got object or namespace element
   *
   * @return checking result
   */
  boolean hasExplicitNamespace();

  /**
   * Check if this is an object method invocation
   *
   * @return result
   */
  boolean isObjectMethod();

  /**
   * Checking for package name with traversing
   *
   * @return boolean
   */
  @Nullable
  String getContextPackageNameHeavy();

  /**
   * @return possible definitions/aliases or empty collection
   * fixme sub resolving should be move in here
   */
  default List<PsiElement> getTargetElements() {
    PerlSubNameElement subNameElement = getSubNameElement();
    if (subNameElement == null) {
      return Collections.emptyList();
    }
    List<PsiElement> targets = ContainerUtil.newArrayList();
    PerlResolveUtil.processElementReferencesResolveResults(pair -> targets.add(pair.first), subNameElement);
    return targets.isEmpty() ? Collections.emptyList() : targets;
  }
}
