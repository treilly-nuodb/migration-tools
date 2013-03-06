/**
 * Copyright (c) 2012, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nuodb.migrator.cli.run;

import com.google.common.collect.Maps;
import com.nuodb.migrator.cli.parse.Group;
import com.nuodb.migrator.cli.parse.Option;
import com.nuodb.migrator.cli.parse.OptionSet;
import com.nuodb.migrator.cli.parse.Parser;
import com.nuodb.migrator.cli.parse.parser.ParserImpl;
import com.nuodb.migrator.jdbc.JdbcConstants;
import com.nuodb.migrator.spec.JdbcConnectionSpec;
import com.nuodb.migrator.spec.ResourceSpec;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.TimeZone;

import static java.util.TimeZone.getTimeZone;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Verifies functionality of the create & parse pairs of methods in {@link CliRunSupport} for creating option & parsing
 * that option to the required spec classes. Correspondingly tested pairs are:
 * <ul>
 *     <li>{@link CliRunSupport#createSourceGroup()} & {@link CliRunSupport#parseSourceGroup(OptionSet, Option)}</li>
 *     <li>{@link CliRunSupport#createTargetGroup()} & {@link CliRunSupport#parseTargetGroup(OptionSet, Option)}</li>
 *     <li>{@link CliRunSupport#createInputGroup()} & {@link CliRunSupport#parseInputGroup(OptionSet, Option)}</li>
 *     <li>{@link CliRunSupport#createOutputGroup()} & {@link CliRunSupport#parseOutputGroup(OptionSet, Option)}</li>
 *     <li>{@link CliRunSupport#createTimeZoneOption()} & {@link CliRunSupport#parseTimeZoneOption(OptionSet, Option)}</li>
 * </ul>
 *
 * @author Sergey Bushik
 */
@Test(groups = "cli.run.support")
public class CliRunSupportTest {

    private Parser parser;
    private CliRunSupport cliRunSupport;

    @BeforeMethod
    public void init() {
        parser = spy(new ParserImpl());
        cliRunSupport = spy(new CliRunSupport());
    }

    @DataProvider(name = "sourceGroupData")
    public Object[][] createSourceGroupData() {
        String[] arguments = {
                "--source.driver=com.mysql.jdbc.Driver",
                "--source.url=jdbc:mysql://localhost:3306/test",
                "--source.username=root",
                "--source.password=",
                "--source.catalog=test",
                "--source.properties=profileSQL=true"
        };

        JdbcConnectionSpec expected = new JdbcConnectionSpec();
        expected.setDriverClassName("com.mysql.jdbc.Driver");
        expected.setUrl("jdbc:mysql://localhost:3306/test");
        expected.setUsername("root");
        expected.setCatalog("test");

        Map<String, Object> properties = Maps.newHashMap();
        properties.put("profileSQL", "true");
        expected.setProperties(properties);

        return new Object[][]{{arguments, expected}};
    }

    @Test(dataProvider = "sourceGroupData")
    public void testSourceGroup(String[] arguments, JdbcConnectionSpec expected) {
        Group group = cliRunSupport.createSourceGroup();
        assertNotNull(group, "Source group of options is required");


        OptionSet options = parser.parse(arguments, group);
        assertNotNull(options, "Option set containing source group options is expected");

        JdbcConnectionSpec actual = cliRunSupport.parseSourceGroup(options, group);
        assertNotNull(actual, "Connection specification is expected");
        assertEquals(actual, expected);
    }

    @DataProvider(name = "targetGroupData")
    public Object[][] createTargetGroupData() {
        String[] arguments = {
                "--target.url=jdbc:com.nuodb://localhost/test",
                "--target.username=dba",
                "--target.password=goalie",
                "--target.schema=hockey"
        };

        JdbcConnectionSpec expected = new JdbcConnectionSpec();
        expected.setDriverClassName(JdbcConstants.NUODB_DRIVER_CLASS_NAME);
        expected.setUrl("jdbc:com.nuodb://localhost/test");
        expected.setUsername("dba");
        expected.setPassword("goalie");
        expected.setSchema("hockey");
        return new Object[][]{{arguments, expected}};
    }

    @Test(dataProvider = "targetGroupData")
    public void testTargetGroup(String[] arguments, JdbcConnectionSpec expected) {
        Group group = cliRunSupport.createTargetGroup();
        assertNotNull(group, "Target group of options is required");

        OptionSet options = parser.parse(arguments, group);

        JdbcConnectionSpec actual = cliRunSupport.parseTargetGroup(options, group);
        assertNotNull(actual, "Connection specification is expected");
        assertEquals(actual, expected);
    }

    @DataProvider(name = "inputGroupData")
    public Object[][] createInputGroupData() {
        String[] arguments = {
                "--input.path=/tmp/dump.cat",
                "--input.csv.encoding=utf-8",
                "--input.csv.delimiter=tab"
        };

        ResourceSpec expected = new ResourceSpec();
        expected.setPath("/tmp/dump.cat");

        Map<String, Object> attributes = Maps.newHashMap();
        attributes.put("csv.encoding", "utf-8");
        attributes.put("csv.delimiter", "tab");
        expected.setAttributes(attributes);
        return new Object[][]{{arguments, expected}};
    }

    @Test(dataProvider = "inputGroupData")
    public void testInputGroup(String[] arguments, ResourceSpec expected) {
        Group group = cliRunSupport.createInputGroup();
        assertNotNull(group, "Input group of options is required");

        OptionSet options = parser.parse(arguments, group);

        ResourceSpec actual = cliRunSupport.parseInputGroup(options, group);
        assertNotNull(actual, "Resource specification is expected");
        assertEquals(actual, expected);
    }

    @DataProvider(name = "outputGroupData")
    public Object[][] createOutputGroupData() {
        String[] arguments = {
                "--output.path=/tmp/dump.cat",
                "--output.type=bson"
        };

        ResourceSpec expected = new ResourceSpec();
        expected.setPath("/tmp/dump.cat");
        expected.setType("bson");
        return new Object[][]{{arguments, expected}};
    }

    @Test(dataProvider = "outputGroupData")
    public void testOutputGroup(String[] arguments, ResourceSpec expected) {
        Group group = cliRunSupport.createOutputGroup();
        assertNotNull(group, "Output group of options is required");

        OptionSet options = parser.parse(arguments, group);

        ResourceSpec actual = cliRunSupport.parseOutputGroup(options, group);
        assertNotNull(actual, "Resource specification is expected");
        assertEquals(actual, expected);
    }

    @DataProvider(name = "timeZoneData")
    public Object[][] createTimeZoneData() {
        return new Object[][]{
                {new String[]{"-tz=UTC"}, getTimeZone("UTC")},
                {new String[]{"--time.zone=EST"}, getTimeZone("EST")},
                {new String[]{"--time.zone=GMT+2"}, getTimeZone("GMT+2")}
        };
    }

    @Test(dataProvider = "timeZoneData")
    public void testTimeZone(String[] arguments, TimeZone expected) {
        Option option = cliRunSupport.createTimeZoneOption();
        assertNotNull(option, "Time zone option is required");

        OptionSet options = parser.parse(arguments, option);

        TimeZone actual = cliRunSupport.parseTimeZoneOption(options, option);
        assertNotNull(actual, "Time zone is expected");
        assertEquals(actual, expected);
    }
}