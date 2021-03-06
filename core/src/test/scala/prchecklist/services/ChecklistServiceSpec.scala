package prchecklist.services

import prchecklist.infrastructure.{PostgresDatabaseComponent, RedisComponent, GitHubHttpClientComponent}
import prchecklist.models._
import prchecklist.repositories.{RepoRepositoryComponent, GitHubRepositoryComponent}
import prchecklist.test._

import com.github.tarao.nonempty.NonEmpty

import org.scalatest._
import org.scalatest.time._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.Duration
import scalaz.concurrent.Task

class ChecklistServiceSpec extends FunSuite with Matchers with OptionValues with concurrent.ScalaFutures
    with WithTestDatabase
    with RepoRepositoryComponent
    with ChecklistServiceComponent
    with PostgresDatabaseComponent
    with TestAppConfig
    with ModelsComponent
    with GitHubRepositoryComponent
    with GitHubHttpClientComponent
    with RedisComponent
    with GitHubConfig {

  def repoRepository = new RepoRepository

  def checklistService = new ChecklistService

  def http = new Http

  def redis = new Redis

  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(5, Millis))

  lazy val (repo, _) = repoRepository.create(GitHubTypes.Repo("motemen/test-repository", false), "<no token>").run

  test("getChecklist && checkChecklist succeeds") {
    val checkerUser = Visitor(login = "test", accessToken = "")

    val pr = GitHubTypes.PullRequestWithCommits(
      pullRequest = Factory.createGitHubPullRequest,
      commits = List(
        GitHubTypes.Commit("", GitHubTypes.CommitDetail(
          """Merge pull request #2 from motemen/feature-a
            |
            |feature-a
          """.stripMargin
        )),
        GitHubTypes.Commit("", GitHubTypes.CommitDetail(
          """Merge pull request #3 from motemen/feature-b
            |
            |feature-b
          """.stripMargin
        ))
      )
    )

    {
      val (checklist, created) = checklistService.getChecklist(repo, pr, stage = "").run
      checklist.checks.get(2).value shouldNot be('checked)
      checklist.checks.get(3).value shouldNot be('checked)
      checklist.checks.get(4) shouldBe 'empty

      checklistService.checkChecklist(checklist, checkerUser, featurePRNumber = 2).run
    }

    {
      val (checklist, created) = checklistService.getChecklist(repo, pr, stage = "").run
      checklist.checks.get(2).value shouldBe 'checked
      checklist.checks.get(3).value shouldNot be('checked)
      checklist.checks.get(4) shouldBe 'empty
      created shouldBe false

      checklistService.checkChecklist(checklist, checkerUser, featurePRNumber = 3).run

    }

    {
      val (checklist, created) = checklistService.getChecklist(repo, pr, stage = "").run
      checklist.checks.get(2).value shouldBe 'checked
      checklist.checks.get(3).value shouldBe 'checked
      checklist.checks.get(4) shouldBe 'empty
      created shouldBe false
    }
  }
}
