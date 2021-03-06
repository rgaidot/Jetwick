/*
 * Copyright 2011 Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jetwick.ui;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class SearchBoxTest extends WicketPagesTestClass {

    public SearchBoxTest() {
    }

    @Test
    public void testGetParams() {
        SearchBox bx = (SearchBox) tester.startPanel(SearchBox.class);
        tester.assertNoErrorMessage();

        // user param
        assertEquals("timetabling", bx.getParams(null, null, "timetabling", null).getString("user"));
        assertEquals("timetabling", bx.getParams(null, 0, "timetabling", null).getString("user"));
        assertEquals(null, bx.getParams(null, 1, "timetabling", null).getString("user"));
        assertEquals("timetabling", bx.getParams(null, 2, "timetabling", null).getString("user"));

        // search param
        assertEquals("all", bx.getParams(null, null, "timetabling", null).getString("search"));
        assertEquals("all", bx.getParams(null, 0, "timetabling", null).getString("search"));
        assertEquals("friends", bx.getParams(null, 1, "timetabling", null).getString("search"));
        assertEquals("user", bx.getParams(null, 2, "timetabling", null).getString("search"));
    }
}