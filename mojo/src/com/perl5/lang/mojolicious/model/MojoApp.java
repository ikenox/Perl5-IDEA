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

package com.perl5.lang.mojolicious.model;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.vfs.VirtualFile;
import com.perl5.lang.mojolicious.MojoBundle;
import com.perl5.lang.mojolicious.MojoIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.regex.Pattern;

public class MojoApp extends MojoProject {
  public MojoApp(@NotNull VirtualFile root) {
    super(root);
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return MojoIcons.MOJO_APP_ICON;
  }

  @NotNull
  @Override
  public String getTypeName() {
    return MojoBundle.message("mojo.app.type.name");
  }

  public static class NameValidator implements InputValidator {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Z](?:\\w|::)+$");

    @Override
    public boolean checkInput(String inputString) {
      return NAME_PATTERN.matcher(inputString).matches();
    }

    @Override
    public boolean canClose(String inputString) {
      return checkInput(inputString);
    }
  }
}
