package fr.obeo.tools.stuart;

import java.io.File;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;

import fr.obeo.tools.stuart.bugzilla.BugzillaLogger;
import fr.obeo.tools.stuart.eclipseforum.EclipseForumsLogger;
import fr.obeo.tools.stuart.gerrit.GerritLogger;
import fr.obeo.tools.stuart.git.GitLogger;
import fr.obeo.tools.stuart.jenkins.JenkinsLogger;
import fr.obeo.tools.stuart.mattermost.MattermostEmitter;
import fr.obeo.tools.stuart.rss.RssLogger;

public class EclipseMattermostInstanceTest {

	private String host = "mattermost-test.eclipse.org";

	@Test
	public void eclipseAnnounces() throws Exception {
		String storage = System.getenv("WORKSPACE");
		if (storage == null) {
			storage = ".";
		}

		String channel = System.getenv("NEWS_CHANNEL");
		if (channel != null) {
			MattermostEmitter emitter = new MattermostEmitter("https", host, channel);

			Date daysAgo = getDateXDaysAgo(15);

			EmitterTrace traceFile = new EmitterTrace(
					new File(storage + "/" + host + "_" + Hashing.sha256().hashString(channel) + "_trace.json"));
			Map<String, Date> trace = traceFile.load();

			List<Post> posts = Lists.newArrayList();
			posts.addAll(
					new RssLogger(new URL("https://dev.eclipse.org/mhonarc/lists/eclipse.org-committers/maillist.rss"),
							daysAgo).get());
			posts.addAll(new RssLogger(new URL("http://planet.eclipse.org/planet/rss20.xml"), daysAgo).get());

			Collections.sort(posts, new Comparator<Post>() {
				public int compare(Post m1, Post m2) {
					return m1.getCreatedAt().compareTo(m2.getCreatedAt());
				}
			});

			for (Post post : posts) {
				send(emitter, trace, post);
			}
			traceFile.evictOldEvents(trace, 60);
			traceFile.save(trace);
		} else {
			Assert.fail("Expecting the NEWS_CHANNEL environment variable to be set");
		}
	}

	@Test
	public void sendEventsToPlatformChans() throws Exception {
		String storage = System.getenv("WORKSPACE");
		if (storage == null) {
			storage = ".";
		}

		String bug_Channel = System.getenv("PLATFORM_BUG_CHANNEL");
		String qa_Channel = System.getenv("PLATFORM_QA_CHANNEL");
		String patch_Channel = System.getenv("PLATFORM_PATCHES_CHANNEL");
		if (qa_Channel != null && bug_Channel != null && patch_Channel != null) {
			MattermostEmitter qaEmitter = new MattermostEmitter("https", host, qa_Channel);

			Date daysAgo = getDateXDaysAgo(3);

			EmitterTrace traceFile = new EmitterTrace(new File(storage + "/" + host + "_platform" + "_trace.json"));
			Map<String, Date> trace = traceFile.load();

			List<Post> posts = Lists.newArrayList();
			posts.addAll(new EclipseForumsLogger(11, daysAgo).forumLog());
			posts.addAll(new EclipseForumsLogger(116, daysAgo).forumLog());
			posts.addAll(new EclipseForumsLogger(106, daysAgo).forumLog());
			posts.addAll(new EclipseForumsLogger(12, daysAgo).forumLog());
			posts.addAll(new EclipseForumsLogger(100, daysAgo).forumLog());
			posts.addAll(new EclipseForumsLogger(15, daysAgo).forumLog());

			Collections.sort(posts, new Comparator<Post>() {
				public int compare(Post m1, Post m2) {
					return m1.getCreatedAt().compareTo(m2.getCreatedAt());
				}
			});

			for (Post post : posts) {
				send(qaEmitter, trace, post);
			}

			MattermostEmitter bugEmitter = new MattermostEmitter("https", host, bug_Channel);
			List<Post> bugzillas = Lists.newArrayList();
			bugzillas.addAll(
					new BugzillaLogger("https://bugs.eclipse.org/bugs", Sets.newHashSet("genie", "genie@eclipse.org"))
							.bugzillaLog(3, Sets.newHashSet("Platform")));

			for (Post post : bugzillas) {
				send(bugEmitter, trace, post);
			}

			List<Post> patches = Lists.newArrayList();
			patches.addAll(new GerritLogger("https://git.eclipse.org/r", 10).getPatchsets(Sets.newHashSet(
					"platform/eclipse.platform", "platform/eclipse.platform.common", "platform/eclipse.platform.debug",
					"platform/eclipse.platform.images", "platform/eclipse.platform.news",
					"platform/eclipse.platform.resources", "platform/eclipse.platform.runtime",
					"platform/eclipse.platform.swt", "platform/eclipse.platform.team", "platform/eclipse.platform.text",
					"platform/eclipse.platform.ua", "platform/eclipse.platform.ui",
					"platform/eclipse.platform.tools")));

			MattermostEmitter patchesEmitter = new MattermostEmitter("https", host, patch_Channel);
			for (Post post : patches) {
				send(patchesEmitter, trace, post);
			}

			traceFile.evictOldEvents(trace, 60);
			traceFile.save(trace);
		} else {
			Assert.fail(
					"Expecting the PLATFORM_QA_CHANNEL, PLATFORM_BUG_CHANNEL,PLATFORM_PATCHES_CHANNEL environment variable to be set");
		}
	}

	@Test
	public void sendEventsToCDTChans() throws Exception {
		String storage = System.getenv("WORKSPACE");
		if (storage == null) {
			storage = ".";
		}

		String qa_Channel = System.getenv("CDT_CHANNEL");
		if (qa_Channel != null) {
			MattermostEmitter qaEmitter = new MattermostEmitter("https", host, qa_Channel);

			Date daysAgo = getDateXDaysAgo(3);

			EmitterTrace traceFile = new EmitterTrace(new File(storage + "/" + host + "_cdtgeneral" + "_trace.json"));
			Map<String, Date> trace = traceFile.load();

			List<Post> posts = Lists.newArrayList();
			posts.addAll(new EclipseForumsLogger(80, daysAgo).forumLog());
			posts.addAll(new RssLogger(new URL("http://stackoverflow.com/feeds/tag/cdt"), daysAgo)
					.setIcon("https://veithen.github.io/images/icon-stackoverflow.svg").get());
			posts.addAll(new RssLogger(new URL("http://stackoverflow.com/feeds/tag/eclipse-cdt"), daysAgo)
					.setIcon("https://veithen.github.io/images/icon-stackoverflow.svg").get());

			Collections.sort(posts, new Comparator<Post>() {
				public int compare(Post m1, Post m2) {
					return m1.getCreatedAt().compareTo(m2.getCreatedAt());
				}
			});

			for (Post post : posts) {
				send(qaEmitter, trace, post);
			}

			traceFile.evictOldEvents(trace, 60);
			traceFile.save(trace);
		} else {
			Assert.fail("Expecting the CDT_CHANNEL environment variable to be set");
		}
	}

	@Test
	public void sendEventsToPackageDrone() throws Exception {
		String storage = System.getenv("WORKSPACE");
		if (storage == null) {
			storage = ".";
		}

		String qa_Channel = System.getenv("PACKAGEDRONE_CHANNEL");
		if (qa_Channel != null) {
			MattermostEmitter emitter = new MattermostEmitter("https", host, qa_Channel);

			Date daysAgo = getDateXDaysAgo(3);

			EmitterTrace traceFile = new EmitterTrace(new File(storage + "/" + host + "_mattermost" + "_trace.json"));
			Map<String, Date> trace = traceFile.load();

			List<Post> posts = Lists.newArrayList();
			posts.addAll(
					new BugzillaLogger("https://bugs.eclipse.org/bugs", Sets.newHashSet("genie", "genie@eclipse.org"))
							.bugzillaLog(3, Sets.newHashSet("Package-Drone")));
			posts.addAll(new GitLogger(new File(storage + "/clones/")).getMergedCommits(daysAgo,
					"https://github.com/eclipse/packagedrone.git", "https://github.com/eclipse/packagedrone/commit/"));
			posts.addAll(new EclipseForumsLogger(318, daysAgo).forumLog());
			posts.addAll(new JenkinsLogger("https://hudson.eclipse.org/package-drone/", daysAgo).getBuildResults());

			posts.addAll(new RssLogger(new URL("https://dentrassi.de/feed/"), daysAgo).get());
			
			Collections.sort(posts, new Comparator<Post>() {
				public int compare(Post m1, Post m2) {
					return m1.getCreatedAt().compareTo(m2.getCreatedAt());
				}
			});

			for (Post post : posts) {
				send(emitter, trace, post);
			}

			traceFile.evictOldEvents(trace, 60);
			traceFile.save(trace);
		} else {
			Assert.fail("Expecting the PACKAGEDRONE_CHANNEL environment variable to be set");
		}
	}

	public static Date getDateXDaysAgo(int nbDays) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -nbDays);
		Date daysAgo = cal.getTime();
		return daysAgo;
	}

	@Test
	public void sendEventsToSiriusPrivateChan() throws Exception {

		String storage = System.getenv("WORKSPACE");
		if (storage == null) {
			storage = ".";
		}

		String channel = System.getenv("MATTERMOST_CHANNEL");
		if (channel != null) {
			MattermostEmitter emitter = new MattermostEmitter("https", host, channel);

			int nbDays = 3;

			Date daysAgo = getDateXDaysAgo(nbDays);

			EmitterTrace traceFile = new EmitterTrace(
					new File(storage + "/" + host + "_" + Hashing.sha256().hashString(channel) + "_trace.json"));
			Map<String, Date> trace = traceFile.load();

			List<Post> posts = Lists.newArrayList();
			posts.addAll(new GitLogger(new File(storage + "/clones/")).getMergedCommits(daysAgo,
					"https://git.eclipse.org/r/sirius/org.eclipse.sirius",
					"https://git.eclipse.org/c/sirius/org.eclipse.sirius.git/commit/?id="));
			posts.addAll(new EclipseForumsLogger(262, daysAgo).forumLog());

			posts.addAll(
					new JenkinsLogger("https://hudson.eclipse.org/sirius/", daysAgo).getBuildResults(trace.keySet()));
			posts.addAll(new GerritLogger("https://git.eclipse.org/r", nbDays)
					.getPatchsets(Sets.newHashSet("sirius/org.eclipse.sirius")));
			posts.addAll(
					new BugzillaLogger("https://bugs.eclipse.org/bugs", Sets.newHashSet("genie", "genie@eclipse.org"))
							.bugzillaLog(3, Sets.newHashSet("Sirius")));

			Collections.sort(posts, new Comparator<Post>() {
				public int compare(Post m1, Post m2) {
					return m1.getCreatedAt().compareTo(m2.getCreatedAt());
				}
			});

			for (Post post : posts) {
				send(emitter, trace, post);
			}
			traceFile.evictOldEvents(trace, 60);
			traceFile.save(trace);
		} else {
			Assert.fail("Expecting the MATTERMOST_CHANNEL environment variable to be set");
		}
	}

	private void send(MattermostEmitter emitter, Map<String, Date> trace, Post post) {
		if (!trace.containsKey(post.getKey())) {
			try {
				System.err.println("Sending :" + post.getKey());
				emitter.accept(MattermostPost.fromGenericPost(post));
				trace.put(post.getKey(), new Date());
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}