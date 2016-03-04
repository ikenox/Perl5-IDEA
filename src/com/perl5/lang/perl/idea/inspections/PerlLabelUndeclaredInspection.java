/*
 * Copyright 2016 Alexandr Evstigneev
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

package com.perl5.lang.perl.idea.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.perl5.lang.perl.psi.PerlLabel;
import com.perl5.lang.perl.psi.PerlLabelDeclaration;
import com.perl5.lang.perl.psi.PerlVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * Created by hurricup on 04.03.2016.
 */
public class PerlLabelUndeclaredInspection extends PerlInspection
{
	@NotNull
	@Override
	public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new PerlVisitor()
		{
			@Override
			public void visitLabel(@NotNull PerlLabel o)
			{
				if (!(o.getParent() instanceof PerlLabelDeclaration))
				{
					PsiReference reference = o.getReference();
					if (reference != null)
					{
						PsiElement declaration = reference.resolve();
						if (declaration == null)
						{
							registerProblem(holder, o, "Unable to find label declaration (possible deprecated usage)");
						}
					}
				}
				super.visitLabel(o);
			}
		};
	}
}
