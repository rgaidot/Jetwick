/**
 * Copyright (C) 2010 Peter Karich <jetwick_@_pannous_._info>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jetwick.ui;

import de.jetwick.es.TweetQuery;
import de.jetwick.es.ElasticTweetSearch;
import de.jetwick.config.Configuration;
import de.jetwick.rmi.RMIClient;
import de.jetwick.es.JetwickQuery;
import de.jetwick.data.JTweet;
import de.jetwick.data.JUser;
import de.jetwick.tw.TwitterSearch;
import de.jetwick.tw.queue.QueueThread;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.wicket.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.Test;
import twitter4j.TwitterException;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class HomePageTest extends WicketPagesTestClass {

    private String uString;
    private String qString;
    private List<JTweet> returnUserTweets;
    private List<JTweet> returnSearchTweets;
    private Collection<JTweet> sentTweets;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        reset();
    }

    public void reset() {
        sentTweets = null;
        uString = "";
        qString = "";
        JUser u = new JUser("peter");
        JUser u2 = new JUser("peter2");
        returnUserTweets = Arrays.asList(new JTweet(3L, "java test2", u2), new JTweet(4L, "java pest2", u2));
        returnSearchTweets = Arrays.asList(new JTweet(1L, "java test", u), new JTweet(2L, "java pest", u));
    }

    @Test
    public void testSelectAndRemove() {
        tester.startPage(TweetSearchPage.class);
        tester.assertNoErrorMessage();

        FormTester formTester = tester.newFormTester("searchbox:searchform");
        formTester.setValue("textField", "java");
        formTester.submit();
        tester.assertNoErrorMessage();

        tester.clickLink("searchbox:searchform:homelink");
        tester.assertNoErrorMessage();
    }

    @Test
    public void testNormalSearch() throws Exception {
//        setUp();
        ElasticTweetSearch search = getInstance(ElasticTweetSearch.class);
        JetwickQuery query = new TweetQuery("timetabling");
        tester.startPage(new TweetSearchPage(query));
        tester.assertNoErrorMessage();

        verify(search).query(new LinkedHashSet<JUser>(), query);
    }

    @Test
    public void testQueueWhenNoResults() throws InterruptedException {
        TweetSearchPage page = getInstance(TweetSearchPage.class);

        QueueThread pkg = page.queueTweets(null, "java", null);
        Thread t = new Thread(pkg);
        t.start();
        t.join();
        assertNotNull(sentTweets);

        // perform normal searchAndGetUsers        
        assertEquals("#java", qString);
        assertEquals("", uString);
    }

    @Test
    public void testQueueWhenUserSearch() throws InterruptedException {
        TweetSearchPage page = getInstance(TweetSearchPage.class);

        QueueThread p = page.queueTweets(null, null, "java");
        p.run();
        assertNotNull(sentTweets);

        assertEquals("", qString);
        assertEquals("#java", uString);
    }

    @Test
    public void testAvoidDuplicateSearchEnqueueing() throws InterruptedException {
        TweetSearchPage page = getInstance(TweetSearchPage.class);

        QueueThread p = page.queueTweets(null, null, "java");
        p.run();
        assertNotNull(sentTweets);
        assertEquals("", qString);
        assertEquals("#java", uString);

        reset();
        p = page.queueTweets(null, null, "Java");
        p.run();
        assertNull(sentTweets);
        assertEquals("", qString);
        assertEquals("", uString);

//        reset();
//        page = getInstance(TweetSearchPage.class);
//        p = page.queueTweets(null, null, "Java");
//        p.run();
//        assertNull(sentTweets);
//        assertEquals("", qString);
//        assertEquals("", uString);
    }

    @Test
    public void testNoNullPointerExcForInstantSearch() throws InterruptedException {
        TweetSearchPage page = getInstance(TweetSearchPage.class);

        // query and user are null and hits == 0 => no background thread is created
        page.init(new TweetQuery(), new PageParameters());
        assertNull(page.getQueueThread());

        page.doSearch(new TweetQuery(), 0, false, true);
        assertNull(page.getQueueThread());
        assertEquals("", uString);
        assertEquals("", qString);
    }

    @Test
    public void testWithDate() throws InterruptedException {
        TweetSearchPage page = getInstance(TweetSearchPage.class);
        PageParameters pp = new PageParameters();
        pp.put("until", "2011-02-01");
        JetwickQuery q = page.createQuery(pp);
        assertEquals("[2011-02-01T00:00:00Z TO *]", q.getFirstFilterQuery("dt"));

        pp = new PageParameters();
        pp.put("until", "2011-02-01T00:00:00Z");
        q = page.createQuery(pp);
        assertEquals("[2011-02-01T00:00:00Z TO *]", q.getFirstFilterQuery("dt"));
    }

    @Test
    public void testWhithNoSolrSearch() throws InterruptedException {
        TweetSearchPage page = getInstance(TweetSearchPage.class);
        page.setTwitterFallback(true);
        page.init(new TweetQuery("java"), new PageParameters());

        page.getQueueThread().run();
        assertNotNull(sentTweets);
        assertEquals("", uString);
        assertEquals("#java", qString);

        // do not trigger background search for the same query
        page.doSearch(new TweetQuery("java"), 0, true);
        assertNull(page.getQueueThread());

        // if only user search then set twitterFallback = true
        reset();
        page.doSearch(new TweetQuery().addFilterQuery("user", "test"), 0, true);
        assertEquals("#test", uString);
        assertEquals("", qString);
        page.getQueueThread().run();

        // if 'normal query' AND 'user search' then set twitterFallback = false but trigger backgr. thread
        reset();
        page.doSearch(new TweetQuery("java").addFilterQuery("user", "test"), 0, true);
        page.getQueueThread().join();
        assertEquals("#test", uString);
        assertEquals("", qString);
    }

    @Override
    protected TwitterSearch createTestTwitterSearch() {
        return new TwitterSearch() {

            @Override
            public boolean isInitialized() {
                return true;
            }

            @Override
            public int getRateLimit() {
                return 100;
            }

            @Override
            public int getRateLimitFromCache() {
                return 100;
            }

            @Override
            public TwitterSearch initTwitter4JInstance(String token, String tokenSec, boolean verify) {
                return this;
            }

            @Override
            public JUser getUser() throws TwitterException {
                return new JUser("testUser");
            }

            @Override
            public long search(String term, Collection<JTweet> result, int tweets, long lastSearch) throws TwitterException {
                qString = "#" + term;
                result.addAll(returnSearchTweets);
                return lastSearch;
            }

            @Override
            public Collection<JTweet> searchAndGetUsers(String queryStr, Collection<JUser> result, int rows, int maxPage) throws TwitterException {
                qString = "#" + queryStr;
                return returnSearchTweets;
            }

            @Override
            public List<JTweet> getTweets(JUser user, Collection<JUser> result, int tweets) throws TwitterException {
                uString = "#" + user.getScreenName();
                return returnUserTweets;
            }
        };
    }

    @Override
    protected RMIClient createRMIClient() {
        return new RMIClient(new Configuration()) {

            @Override
            public RMIClient init() {
                return this;
            }

            @Override
            public void send(JTweet tweet) throws RemoteException {
                sentTweets = Arrays.asList(tweet);
            }

            @Override
            public void send(Collection<JTweet> tweets) throws RemoteException {
                sentTweets = tweets;
            }
        };
    }
//    @Override
//    protected ElasticTweetSearch createSolrTweetSearch() {
//        if (ownSolrTweetSearch == null)
//            return super.createSolrTweetSearch();
//
//        return ownSolrTweetSearch;
//    }
}
