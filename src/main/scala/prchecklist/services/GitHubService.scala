package prchecklist.services

import prchecklist.models._
import prchecklist.utils.GitHubHttpClient

import scalaz.concurrent.Task
import scalaz.syntax.applicative._

class GitHubService(val githubHttpClient: GitHubHttpClient) {
  def getRepo(owner: String, name: String): Task[GitHubTypes.Repo] = {
    githubHttpClient.getJson[GitHubTypes.Repo](s"/repos/$owner/$name")
  }

  // https://developer.github.com/v3/issues/comments/#create-a-comment
  def addIssueComment(repoFullName: String, issueNumber: Int, body: String): Task[Unit] = {
    githubHttpClient.postJson(
      s"/repos/$repoFullName/issues/$issueNumber/comments",
      GitHubTypes.IssueComment(body)
    )
  }

  // https://developer.github.com/v3/repos/statuses/#create-a-status
  def setCommitStatus(repoFullName: String, sha: String, status: GitHubTypes.CommitStatus): Task[Unit] = {
    githubHttpClient.postJson(
      s"/repos/$repoFullName/statuses/$sha",
      status
    )
  }

  def getPullRequestWithCommits(repo: Repo, number: Int): Task[GitHubTypes.PullRequestWithCommits] = {
    val getPullRequestTask =
      githubHttpClient.getJson[GitHubTypes.PullRequest](s"/repos/${repo.fullName}/pulls/$number")

    // TODO: paging
    val getPullRequestCommitsTask =
      githubHttpClient.getJson[List[GitHubTypes.Commit]](s"/repos/${repo.fullName}/pulls/$number/commits?per_page=100")

    (getPullRequestTask |@| getPullRequestCommitsTask) apply (GitHubTypes.PullRequestWithCommits)
  }

  def listReleasePullRequests(repo: Repo): Task[List[GitHubTypes.PullRequest]] = {
    githubHttpClient.getJson[List[GitHubTypes.PullRequest]](s"/repos/${repo.fullName}/pulls?base=master&state=all")
  }
}
