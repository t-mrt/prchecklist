package prchecklist.utils

trait AppConfig {
  val githubClientId: String

  val githubClientSecret: String

  val githubDomain: String = "github.com"

  val githubDefaultToken: String

  val databaseUrl: String

  val redisUrl: String

  val httpAllowUnsafeSSL: Boolean = false

  implicit class FullUrlRepo(val repo: prchecklist.models.Repo)
      extends prchecklist.models.Repo(repo.id, repo.owner, repo.name, repo.defaultAccessToken) {
    def pullRequestUrl(number: Int) = s"$url/pull/$number"
    def url = s"https://${githubDomain}/${repo.fullName}"
  }

  implicit class FullUrlReleaseChecklist(val checklist: prchecklist.models.ReleaseChecklist)
      extends prchecklist.models.ReleaseChecklist(checklist.id, checklist.repo, checklist.pullRequest, checklist.stage, checklist.featurePullRequests, checklist.checks) {
    def pullRequestUrl(number: Int) = checklist.repo.pullRequestUrl(number)
  }
}

trait AppConfigFromEnv extends AppConfig {
  private def envOrSystemProp(envKey: String, propKey: String, default: => String): String = {
    Option(System.getenv(envKey)) orElse Option(System.getProperty(propKey)) getOrElse default
  }

  private def envOrSystemProp(envKey: String, propKey: String): String =
    envOrSystemProp(envKey, propKey, { throw new Error(s"$propKey or $envKey must be set") })

  override val githubClientId = envOrSystemProp("GITHUB_CLIENT_ID", "github.clientId")

  override val githubClientSecret = envOrSystemProp("GITHUB_CLIENT_SECRET", "github.clientSecret")

  override val githubDomain = envOrSystemProp("GITHUB_DOMAIN", "github.domain", "github.com")

  override val githubDefaultToken = envOrSystemProp("GITHUB_DEFAULT_TOKEN", "github.defaultToken", "")

  override val databaseUrl = envOrSystemProp("DATABASE_URL", "database.url", "jdbc:postgresql:prchecklist_local")

  override val redisUrl = envOrSystemProp("REDIS_URL", "redis.url", "redis://127.0.0.1:6379")

  override val httpAllowUnsafeSSL = envOrSystemProp("HTTP_ALLOW_UNSAFE_SSL", "http.allowUnsafeSSL", "") == "true"
}
