/*
 * Copyright 2018-2019 scala-steward contributors
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

package org.scalasteward.core.github

import cats.implicits._
import org.http4s.Uri
import org.scalasteward.core.application.Config
import org.scalasteward.core.git.GitAlg
import org.scalasteward.core.github.data.{Repo, RepoOut}
import org.scalasteward.core.util
import org.scalasteward.core.util.MonadThrowable

trait GitHubRepoAlg[F[_]] {
  def clone(repo: Repo, repoOut: RepoOut): F[Unit]

  def syncFork(repo: Repo, repoOut: RepoOut): F[RepoOut]
}

object GitHubRepoAlg {
  def create[F[_]: MonadThrowable](config: Config, gitAlg: GitAlg[F]): GitHubRepoAlg[F] =
    new GitHubRepoAlg[F] {
      override def clone(repo: Repo, repoOut: RepoOut): F[Unit] =
        for {
          _ <- gitAlg.clone(repo, withLogin(repoOut.clone_url))
          _ <- gitAlg.setAuthor(repo, config.gitAuthor)
        } yield ()

      override def syncFork(repo: Repo, repoOut: RepoOut): F[RepoOut] =
        if (config.doNotFork) repoOut.pure[F]
        else {
          for {
            parent <- repoOut.parentOrRaise[F]
            _ <- gitAlg.syncFork(repo, withLogin(parent.clone_url), parent.default_branch)
          } yield parent
        }

      val withLogin: Uri => Uri =
        util.uri.withUserInfo.set(config.gitHubLogin)
    }
}
