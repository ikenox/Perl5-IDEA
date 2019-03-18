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

package com.perl5.lang.perl.idea.refactoring;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.perl5.PerlBundle;
import com.perl5.lang.perl.PerlParserDefinition;
import com.perl5.lang.perl.psi.*;
import com.perl5.lang.perl.psi.utils.PerlPsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.perl5.lang.perl.lexer.PerlElementTypesGenerated.*;

public class PerlIntroduceVariableHandler implements RefactoringActionHandler {
  private static final Logger LOG = Logger.getInstance(PerlIntroduceVariableHandler.class);
  private static final TokenSet UNINTRODUCIBLE_TOKENS = TokenSet.create(
    CONDITION_EXPR, NESTED_CALL, PARENTHESISED_EXPR,
    VARIABLE_DECLARATION_LEXICAL, VARIABLE_DECLARATION_GLOBAL, VARIABLE_DECLARATION_LOCAL
  );
  private static final TokenSet SEQUENTINAL_TOKENS = TokenSet.create(
    COMMA_SEQUENCE_EXPR, DEREF_EXPR,
    ADD_EXPR, MUL_EXPR, SHIFT_EXPR, BITWISE_AND_EXPR, BITWISE_OR_XOR_EXPR, AND_EXPR, OR_EXPR, LP_AND_EXPR, LP_OR_XOR_EXPR
  );

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, DataContext dataContext) {
    List<PerlIntroduceTarget> targets = computeIntroduceTargets(editor, file);
    if (targets.isEmpty()) {
      showErrorMessage(project, editor, RefactoringBundle.getCannotRefactorMessage(PerlBundle.message("perl.introduce.no.target")));
      return;
    }

    if (targets.size() > 1) {
      IntroduceTargetChooser.showIntroduceTargetChooser(
        editor,
        targets,
        new Pass<PerlIntroduceTarget>() {
          @Override
          public void pass(PerlIntroduceTarget target) {
            introduceTarget(target, editor, file, dataContext);
          }
        },
        PerlBundle.message("perl.introduce.expressions"),
        -1);
    }
    else {
      introduceTarget(targets.iterator().next(), editor, file, dataContext);
    }
  }

  /**
   * Collects occurrences of selected {@code target}, suggest to replace all or one, and going on
   */
  private void introduceTarget(@NotNull PerlIntroduceTarget target,
                               @NotNull Editor editor,
                               @NotNull PsiFile file,
                               DataContext dataContext) {
    List<PerlIntroduceTarget> allOccurrences = collectOccurrences(target);
    if (allOccurrences.size() > 1) {
      Map<PsiElement, PerlIntroduceTarget> occurrencesMap = ContainerUtil.map2Map(allOccurrences, it -> Pair.create(it.getPlace(), it));

      new OccurrencesChooser<PsiElement>(editor) {
        @Override
        protected TextRange getOccurrenceRange(PsiElement occurrence) {
          return occurrencesMap.get(occurrence).getTextRange();
        }
      }.showChooser(
        target.getPlace(),
        new ArrayList<>(occurrencesMap.keySet()),
        new Pass<OccurrencesChooser.ReplaceChoice>() {
          @Override
          public void pass(OccurrencesChooser.ReplaceChoice replaceChoice) {
            introduceTarget(target, allOccurrences, replaceChoice, editor, file, dataContext);
          }
        }
      );
    }
    introduceTarget(target, allOccurrences, OccurrencesChooser.ReplaceChoice.NO, editor, file, dataContext);
  }

  private void introduceTarget(@NotNull PerlIntroduceTarget target,
                               @NotNull List<PerlIntroduceTarget> allTargets,
                               @NotNull OccurrencesChooser.ReplaceChoice replaceChoice,
                               @NotNull Editor editor,
                               @NotNull PsiFile file,
                               DataContext dataContext) {
    LOG.warn("Introducing " + target.render());
  }

  /**
   * @return occurrences of expression to introduce, represented by {@code target}
   */
  @NotNull
  public List<PerlIntroduceTarget> collectOccurrences(@NotNull PerlIntroduceTarget target) {
    PsiElement targetElement = target.getPlace();
    PsiElement scope = PsiTreeUtil.getParentOfType(targetElement, PerlSubDefinitionElement.class);
    if (scope == null) {
      scope = PsiTreeUtil.getParentOfType(targetElement, PerlNamespaceDefinitionElement.class);
    }
    if (scope == null) {
      return Collections.singletonList(target);
    }

    List<PerlIntroduceTarget> result = new ArrayList<>();
    scope.accept(new PerlRecursiveVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        PerlIntroduceTarget elementTarget = null;
        if (targetElement instanceof PerlDerefExpression) {
          elementTarget = computeDerefTargetIfSame((PerlDerefExpression)targetElement, target.getTextRangeInElement(), element);
        }
        else if (targetElement instanceof PerlStringList) {
          elementTarget = computeStringListTargetIfSame((PerlStringList)targetElement, target.getTextRangeInElement(), element);
        }
        else if (target.isFullRange() && PerlPsiUtil.areElementsSame(targetElement, element)) {
          elementTarget = PerlIntroduceTarget.create(element);
        }

        if (elementTarget != null) {
          result.add(elementTarget);
        }
        else {
          super.visitElement(element);
        }
      }
    });
    return result;
  }

  /**
   * @return an introduce target of an {@code element} if it matches with {@code example} within {@code rangeInExample}, null otherwise
   */
  @Nullable
  private PerlIntroduceTarget computeStringListTargetIfSame(@NotNull PerlStringList example,
                                                            @NotNull TextRange rangeInExample,
                                                            @NotNull PsiElement element) {
    List<PsiElement> exampleStrings = new ArrayList<>();
    if (!PerlPsiUtil.collectStringElementsRecursivelyStrict(example, exampleStrings)) {
      return null;
    }

    List<PsiElement> stringsToSearch =
      ContainerUtil.filter(exampleStrings, it -> rangeInExample.contains(it.getTextRangeInParent()));
    if (stringsToSearch.isEmpty()) {
      return null;
    }

    List<PsiElement> elementStrings = new ArrayList<>();
    PerlPsiUtil.collectStringElementsRecursivelyStrict(example, elementStrings);

    if (stringsToSearch.size() > elementStrings.size()) {
      return null;
    }

    PsiElement firstStringToSearch = stringsToSearch.get(0);
    for (int startIndex = 0; startIndex < elementStrings.size() - stringsToSearch.size(); startIndex++) {
      PsiElement firstElementString = elementStrings.get(startIndex);
      if (!PerlPsiUtil.areElementsSame(firstStringToSearch, firstElementString)) {
        continue;
      }
      int offset = 1;
      for (; offset < stringsToSearch.size(); offset++) {
        if (!PerlPsiUtil.areElementsSame(stringsToSearch.get(offset), elementStrings.get(startIndex + offset))) {
          break;
        }
      }
      if (offset == stringsToSearch.size()) {
        // matches
        return startIndex == 0 && offset == elementStrings.size() ?
               PerlIntroduceTarget.create(element) :
               PerlIntroduceTarget.create(element, TextRange.create(
                 firstElementString.getTextRange().getStartOffset(),
                 elementStrings.get(startIndex + offset - 1).getTextRange().getEndOffset()
               ).shiftLeft(element.getTextRange().getStartOffset()));
      }
    }
    return null;
  }

  /**
   * @return an introduce target of an {@code element} if it matches with {@code example} within {@code rangeInExample}, null otherwise
   */
  @Nullable
  private PerlIntroduceTarget computeDerefTargetIfSame(@NotNull PerlDerefExpression example,
                                                       @NotNull TextRange rangeInExample,
                                                       @NotNull PsiElement element) {
    if (!(element instanceof PerlDerefExpression)) {
      return null;
    }
    List<PsiElement> targetChildren = ContainerUtil.filter(example.getChildren(), it -> rangeInExample.contains(it.getTextRangeInParent()));
    PsiElement[] elementChildren = element.getChildren();
    if (elementChildren.length < targetChildren.size()) {
      return null;
    }
    for (int i = 0; i < targetChildren.size(); i++) {
      if (!PerlPsiUtil.areElementsSame(targetChildren.get(i), elementChildren[i])) {
        return null;
      }
    }
    return PerlIntroduceTarget
      .create(element, TextRange.create(0, elementChildren[targetChildren.size() - 1].getTextRangeInParent().getEndOffset()));
  }

  /**
   * @return List of possible introduce targets for {@code file} opened in {@code editor}
   */
  @NotNull
  public List<PerlIntroduceTarget> computeIntroduceTargets(@NotNull Editor editor, @NotNull PsiFile file) {
    if (editor.getSelectionModel().hasSelection()) {
      return computeIntroduceTargetsFromSelection(editor, file);
    }

    List<PerlIntroduceTarget> targets = new ArrayList<>();
    int caretOffset = editor.getCaretModel().getOffset();
    PsiPerlExpr run = PsiTreeUtil.findElementOfClassAtOffset(file, caretOffset, PsiPerlExpr.class, false);
    while (run != null) {
      IElementType elementType = PsiUtilCore.getElementType(run);
      if (SEQUENTINAL_TOKENS.contains(elementType)) {
        for (PsiElement child : run.getChildren()) {
          TextRange childTextRange = child.getTextRange();
          if (childTextRange.contains(caretOffset) || childTextRange.getStartOffset() > caretOffset) {
            targets.add(PerlIntroduceTarget.create(run, child));
          }
        }
      }
      else if (run instanceof PerlQuoted) {
        computeTargetFromPerlQuotedByCaret((PerlQuoted)run, caretOffset, targets);
      }
      else if (!UNINTRODUCIBLE_TOKENS.contains(elementType)) {
        targets.add(PerlIntroduceTarget.create(run));
      }
      run = PsiTreeUtil.getParentOfType(run, PsiPerlExpr.class);
    }
    return targets;
  }

  /**
   * Compute target for quoted entities(perl strings or string lists) by caret position.
   */
  public void computeTargetFromPerlQuotedByCaret(@NotNull PerlQuoted perlQuotedExpr,
                                                 int caretOffset,
                                                 @NotNull List<PerlIntroduceTarget> result) {
    PsiElement stringRun = perlQuotedExpr.getOpenQuoteElement();
    if (stringRun != null) {
      PsiElement closeQuote = perlQuotedExpr.getCloseQuoteElement();
      PsiElement firstStringElement = stringRun.getNextSibling();
      while ((stringRun = stringRun.getNextSibling()) != null && !stringRun.equals(closeQuote)) {
        IElementType stringRunElementType = PsiUtilCore.getElementType(stringRun);
        if (stringRunElementType == TokenType.WHITE_SPACE) {
          continue;
        }
        TextRange stringRunTextRange = stringRun.getTextRange();
        if (stringRunTextRange.contains(caretOffset) || stringRunTextRange.getStartOffset() > caretOffset) {
          if (PerlParserDefinition.LITERALS.contains(stringRunElementType)) {
            String stringRunText = stringRun.getText();
            boolean isLastWhiteSpace = true;
            for (int i = 0; i < stringRunText.length(); i++) {
              boolean isCurrentWhiteSpace = Character.isWhitespace(stringRunText.charAt(i));
              int substringEndOffsetInParent = stringRun.getStartOffsetInParent() + i;
              if (isLastWhiteSpace != isCurrentWhiteSpace && isCurrentWhiteSpace &&
                  substringEndOffsetInParent + stringRunTextRange.getStartOffset() > caretOffset) {
                result.add(PerlIntroduceTarget.create(perlQuotedExpr, firstStringElement.getStartOffsetInParent(),
                                                      substringEndOffsetInParent));
              }
              isLastWhiteSpace = isCurrentWhiteSpace;
            }
            if (!isLastWhiteSpace) {
              result.add(PerlIntroduceTarget.create(perlQuotedExpr, firstStringElement.getStartOffsetInParent(),
                                                    stringRun.getStartOffsetInParent() + stringRunText.length()));
            }
          }
          else {
            result.add(PerlIntroduceTarget.create(perlQuotedExpr, firstStringElement, stringRun));
          }
        }
      }
    }
    result.add(PerlIntroduceTarget.create(perlQuotedExpr));
  }

  @NotNull
  private List<PerlIntroduceTarget> computeIntroduceTargetsFromSelection(Editor editor, PsiFile file) {
    SelectionModel selectionModel = editor.getSelectionModel();
    int selectionStart = selectionModel.getSelectionStart();
    int selectionEnd = selectionModel.getSelectionEnd();
    PsiElement startElement = file.findElementAt(selectionStart);
    PsiElement endElement = file.findElementAt(selectionEnd > selectionStart ? selectionEnd - 1 : selectionEnd);
    if (startElement == null || endElement == null) {
      return Collections.emptyList();
    }
    PsiElement commonParent = PsiTreeUtil.findCommonParent(startElement, endElement);
    PsiElement wrappingExpression = PsiTreeUtil.getParentOfType(commonParent, PsiPerlExpr.class, false);
    while (wrappingExpression != null) {
      if (!UNINTRODUCIBLE_TOKENS.contains(PsiUtilCore.getElementType(wrappingExpression))) {
        break;
      }
      wrappingExpression = PsiTreeUtil.getParentOfType(wrappingExpression, PsiPerlExpr.class, true);
    }
    if (wrappingExpression == null) {
      return Collections.emptyList();
    }

    TextRange selectionRange = TextRange.create(selectionStart, selectionEnd);
    IElementType wrappingExpressionElementType = PsiUtilCore.getElementType(wrappingExpression);
    if (wrappingExpression instanceof PsiPerlStringBare) {
      TextRange intersectedRange = selectionRange.intersection(wrappingExpression.getTextRange());
      if (intersectedRange == null) {
        return Collections.emptyList();
      }
      return Collections.singletonList(PerlIntroduceTarget.create(
        wrappingExpression, intersectedRange.shiftLeft(wrappingExpression.getTextOffset())));
    }
    if (wrappingExpression instanceof PerlQuoted) {
      return computeTargetsForQuotedExpressionsFromSelection(wrappingExpression, selectionRange);
    }
    else if (SEQUENTINAL_TOKENS.contains(wrappingExpressionElementType)) {
      return computeTargetsForSequentialExpressionsFromSelection(wrappingExpression, selectionRange);
    }
    return Collections.singletonList(PerlIntroduceTarget.create(wrappingExpression));
  }

  /**
   * Computes targets for quoted entities, strings and string lists from selection.
   */
  @NotNull
  private List<PerlIntroduceTarget> computeTargetsForQuotedExpressionsFromSelection(@NotNull PsiElement wrappingExpression,
                                                                                    @NotNull TextRange selectionRange) {
    int selectionStart = selectionRange.getStartOffset();
    int selectionEnd = selectionRange.getEndOffset();
    boolean canBreakTokens = !(wrappingExpression instanceof PerlStringList);
    PsiElement run = ((PerlQuoted)wrappingExpression).getOpenQuoteElement();
    if (run == null) {
      return Collections.emptyList();
    }
    PsiElement closeQuote = ((PerlQuoted)wrappingExpression).getCloseQuoteElement();
    if (selectionRange.contains(run.getTextRange()) && (closeQuote == null || selectionRange.contains(closeQuote.getTextRange()))) {
      return Collections.singletonList(PerlIntroduceTarget.create(wrappingExpression));
    }

    int startOffset = -1;
    int endOffset = -1;
    while ((run = run.getNextSibling()) != null) {
      if (run.equals(closeQuote)) {
        break;
      }
      IElementType runElementType = PsiUtilCore.getElementType(run);
      if (runElementType == TokenType.WHITE_SPACE) {
        continue;
      }
      TextRange runTextRange = run.getTextRange();
      if (runTextRange.getEndOffset() <= selectionStart || runTextRange.getStartOffset() >= selectionEnd) {
        continue;
      }

      if (startOffset < 0) {
        startOffset = run.getStartOffsetInParent();
        if (canBreakTokens && selectionStart > runTextRange.getStartOffset() && PerlParserDefinition.LITERALS.contains(runElementType)) {
          startOffset += selectionStart - runTextRange.getStartOffset();
        }
      }
      endOffset = run.getStartOffsetInParent() + run.getTextLength();
      if (canBreakTokens && selectionEnd < runTextRange.getEndOffset() && PerlParserDefinition.LITERALS.contains(runElementType)) {
        endOffset -= runTextRange.getEndOffset() - selectionEnd;
      }
    }
    return startOffset < 0 || endOffset < 0 ? Collections.emptyList() :
           Collections.singletonList(PerlIntroduceTarget.create(wrappingExpression, startOffset, endOffset));
  }

  /**
   * Computes targets for sequential elements from selection: dereference chains, comma sequences, additions, etc.
   */
  @NotNull
  private List<PerlIntroduceTarget> computeTargetsForSequentialExpressionsFromSelection(@NotNull PsiElement wrappingExpression,
                                                                                        @NotNull TextRange selectionRange) {
    PsiElement[] children = wrappingExpression.getChildren();
    PsiElement firstChildToInclude =
      PsiUtilCore.getElementType(wrappingExpression) == DEREF_EXPR ? wrappingExpression.getFirstChild() : null;
    PsiElement lastChildToInclude = null;
    for (PsiElement child : children) {
      TextRange childTextRange = child.getTextRange();
      if (!selectionRange.intersectsStrict(childTextRange)) {
        continue;
      }
      if (firstChildToInclude == null) {
        firstChildToInclude = child;
      }
      lastChildToInclude = child;
    }
    if (firstChildToInclude == null || lastChildToInclude == null) {
      return Collections.emptyList();
    }

    return Collections.singletonList(PerlIntroduceTarget.create(wrappingExpression, firstChildToInclude, lastChildToInclude));
  }

  protected void showErrorMessage(@NotNull Project project, Editor editor, @NotNull String message) {
    CommonRefactoringUtil
      .showErrorHint(project, editor, message, RefactoringBundle.message("introduce.variable.title"), HelpID.INTRODUCE_VARIABLE);
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {

  }
}
