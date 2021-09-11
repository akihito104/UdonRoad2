/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

import { danger, fail, markdown, message, peril, schedule, warn } from 'danger'

const changedLine = danger.github.pr.additions + danger.github.pr.deletions;
if (changedLine > 500) {
  warn("Big PR, try to keep changes smaller if you can");
}

if (danger.github.pr.title.includes("WIP")) {
  warn("PR is classed as Work in Progress");
}
