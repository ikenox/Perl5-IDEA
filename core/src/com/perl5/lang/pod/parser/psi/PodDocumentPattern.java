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

package com.perl5.lang.pod.parser.psi;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.text.CharSequenceSubSequence;
import com.perl5.lang.pod.parser.psi.mixin.PodFormatterX;
import com.perl5.lang.pod.parser.psi.mixin.PodSectionItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pattern for searching through the pod documentation
 */
public class PodDocumentPattern {
  private String myItemPattern;    // pattern to search items
  private String myHeadingPattern; // pattern to search headers
  private String myIndexKey;    // index key to search. If both defined - first wins
  private boolean myExactMatch = false;    // set to true if need exact match, instead of startsWith

  private PodDocumentPattern() {
  }

  public boolean accepts(@Nullable PsiElement element) {
    return element != null && (acceptsItem(element) || acceptsIndex(element) || acceptsHeading(element));
  }

  protected boolean acceptsItem(@NotNull PsiElement element) {
    if (getItemPattern() != null && element instanceof PodSectionItem) {
      String title = ((PodTitledSection)element).getTitleText();
      if (StringUtil.isNotEmpty(title)) {
        return matches(title, getItemPattern(), myExactMatch);
      }
    }
    return false;
  }

  protected boolean acceptsHeading(@NotNull PsiElement element) {
    if (getHeadingPattern() != null && element instanceof PodTitledSection && ((PodTitledSection)element).isHeading()) {
      String title = ((PodTitledSection)element).getTitleText();
      if (StringUtil.isNotEmpty(title)) {
        return matches(title, getHeadingPattern(), myExactMatch);
      }
    }
    return false;
  }

  @Contract(pure = true)
  @Nullable
  public String getItemPattern() {
    return myItemPattern;
  }

  @NotNull
  public PodDocumentPattern withItemPattern(String itemPattern) {
    myItemPattern = StringUtil.nullize(itemPattern);
    return this;
  }

  protected boolean acceptsIndex(@NotNull PsiElement element) {
    if (getIndexKey() != null && element instanceof PodFormatterX) {
      return getIndexKey().equals(((PodFormatterX)element).getTitleText());
    }
    return false;
  }

  @Contract(pure = true)
  @Nullable
  public String getHeadingPattern() {
    return myHeadingPattern;
  }

  @NotNull
  public PodDocumentPattern withHeadingPattern(String pattern) {
    myHeadingPattern = StringUtil.nullize(pattern);
    return this;
  }

  public PodDocumentPattern withExactMatch() {
    myExactMatch = true;
    return this;
  }

  @Contract(pure = true)
  @Nullable
  public String getIndexKey() {
    return myIndexKey;
  }

  @NotNull
  public PodDocumentPattern withIndexPattern(String indexKey) {
    myIndexKey = indexKey;
    return this;
  }

  private static boolean matches(@NotNull String text, @NotNull String pattern, boolean exactMatch) {
    if (!StringUtil.startsWith(text, pattern)) {
      return false;
    }
    if (text.length() == pattern.length()) {
      return true;
    }
    if (exactMatch) {
      return StringUtil.isEmptyOrSpaces(new CharSequenceSubSequence(text, pattern.length(), text.length()));
    }
    // this mimics word boundary
    boolean isNextCharIdentifier = Character.isUnicodeIdentifierPart(text.charAt(pattern.length()));
    boolean isPrevCharIdentifier = Character.isUnicodeIdentifierPart(text.charAt(pattern.length() - 1));
    return isNextCharIdentifier != isPrevCharIdentifier;
  }

  @NotNull
  public static PodDocumentPattern itemPattern(@NotNull String itemText) {
    return new PodDocumentPattern().withItemPattern(itemText);
  }

  @NotNull
  public static PodDocumentPattern indexPattern(@NotNull String itemText) {
    return new PodDocumentPattern().withIndexPattern(itemText);
  }

  @NotNull
  public static PodDocumentPattern headingAndItemPattern(@NotNull String itemText) {
    return new PodDocumentPattern().withItemPattern(itemText).withHeadingPattern(itemText);
  }

  @NotNull
  public static PodDocumentPattern exactAnythingPattern(@NotNull String text) {
    return new PodDocumentPattern().withExactMatch().withItemPattern(text).withIndexPattern(text).withHeadingPattern(text);
  }
}

