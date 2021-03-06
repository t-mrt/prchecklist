package prchecklist.services

import org.scalatest.{ FunSuite, Matchers }
import org.scalatest.mock.MockitoSugar

import org.mockito.Mockito._
import prchecklist.infrastructure.{RedisComponent, GitHubHttpClientComponent}

import prchecklist.models._
import prchecklist.repositories.GitHubRepositoryComponent
import prchecklist.test._
import prchecklist.utils._

import scalaz.concurrent.Task

class GitHubServiceSpec extends FunSuite with Matchers with MockitoSugar
    with GitHubRepositoryComponent
    with GitHubHttpClientComponent
    with RedisComponent
    with TestAppConfig
    with ModelsComponent
    with GitHubConfig
    with HttpComponent {

  override def redis = new Redis

  override def http = new Http

  test("getPullRequestWithCommits") {

    val mockedClient = mock[GitHubHttpClient]

    when(
      mockedClient.getJson[GitHubTypes.PullRequest]("/repos/test-owner/test-name/pulls/47")
    ) thenReturn Task {
        Factory.createGitHubPullRequest.copy(commits = 0)
      }

    when(
      mockedClient.getJson[List[GitHubTypes.Commit]]("/repos/test-owner/test-name/commits?sha=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx&per_page=100&page=1")
    ) thenReturn Task {
        List()
      }

    val githubRepository = new GitHubRepository {
      override val client = mockedClient
    }
    val prWithCommits = githubRepository.getPullRequestWithCommits(Repo(0, "test-owner", "test-name", ""), 47).run

    // TODO: redis
  }

  test("getPullRequestCommitsPaged") {
    val mockedClient = mock[GitHubHttpClient]

    when(
      mockedClient.getJson[List[GitHubTypes.Commit]]("/repos/test-owner/test-name/commits?sha=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx&per_page=100&page=1")
    ) thenReturn Task {
        (1 to 100).map { _ => Factory.createGitHubCommit }.toList
      }

    when(
      mockedClient.getJson[List[GitHubTypes.Commit]]("/repos/test-owner/test-name/commits?sha=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx&per_page=100&page=2")
    ) thenReturn Task {
        (1 to 10).map { _ => Factory.createGitHubCommit }.toList
      }

    val githubRepository = new GitHubRepository {
      override val client = mockedClient
    }
    val prCommits = githubRepository.getPullRequestCommitsPaged(
      Repo(0, "test-owner", "test-name", ""),
      Factory.createGitHubPullRequest.copy(number = 47, head = Factory.createGitHubCommitRef, commits = 101)
    ).run
    prCommits should have length 101
  }
}
