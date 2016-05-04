/*

Derby - Class org.apache.derbyTesting.functionTests.tests.upgradeTests.Changes10_13

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package org.apache.derbyTesting.functionTests.tests.upgradeTests;

import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.Test;
import org.apache.derbyTesting.junit.BaseTestSuite;
import org.apache.derbyTesting.junit.JDBC;


/**
 * Upgrade test cases for 10.13.
 */
public class Changes10_13 extends UpgradeChange
{

    //////////////////////////////////////////////////////////////////
    //
    // CONSTANTS
    //
    //////////////////////////////////////////////////////////////////

    private static final String SYNTAX_ERROR = "42X01";
    private static final String UPGRADE_REQUIRED = "XCL47";
    private static final String CANNOT_ALTER_NON_IDENTITY_COLUMN = "42Z29";
    private static final String CANNOT_MODIFY_ALWAYS_IDENTITY_COLUMN = "42Z23";

    //////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTOR
    //
    //////////////////////////////////////////////////////////////////

    public Changes10_13(String name) {
        super(name);
    }

    //////////////////////////////////////////////////////////////////
    //
    // JUnit BEHAVIOR
    //
    //////////////////////////////////////////////////////////////////

    /**
     * Return the suite of tests to test the changes made in 10.13.
     *
     * @param phase an integer that indicates the current phase in
     *              the upgrade test.
     * @return the test suite created.
     */
    public static Test suite(int phase) {
        return new BaseTestSuite(Changes10_13.class, "Upgrade test for 10.13");
    }

    //////////////////////////////////////////////////////////////////
    //
    // TESTS
    //
    //////////////////////////////////////////////////////////////////

    /**
     * Test the addition of support for changing identity columns
     * from ALWAYS to BY DEFAULT and vice versa via
     * an ALTER TABLE statement. DERBY-6882.
     */
    public void testAlterTableSetGenerated() throws SQLException {
        Statement s = createStatement();

        // GENERATED BY DEFAULT not supported prior to 10.1
        if (!oldAtLeast(10, 1)) { return; }

        // 10.11 upgraded all identity columns to be backed by sequences
        boolean atLeast10_11 = oldAtLeast(10,11);
        
        switch (getPhase()) {
            case PH_CREATE:
                s.execute("create table t_always_6882(a int generated always as identity, b int)");
                s.execute("create table t_default_6882(a int generated by default as identity, b int)");
                s.execute("create table t_none_6882(a int, b int)");
                s.execute("insert into t_always_6882(b) values (100)");
                s.execute("insert into t_default_6882(b) values (100)");
                assertCompileError
                  (
                   CANNOT_MODIFY_ALWAYS_IDENTITY_COLUMN,
                   "insert into t_always_6882(a, b) values (-1, -100)"
                   );
                s.execute("insert into t_default_6882(a, b) values (-1, -100)");
                break;

            case PH_SOFT_UPGRADE:
                // We only support the SET GENERATED clause if the database
                // is at level 10.11 or higher.
                if (atLeast10_11)
                {
                    assertCompileError
                      (
                       CANNOT_ALTER_NON_IDENTITY_COLUMN,
                       "alter table t_none_6882 alter column a set generated by default"
                       );
                    s.execute("alter table t_always_6882 alter column a set generated by default");
                    s.execute("alter table t_default_6882 alter column a set generated always");
                    s.execute("insert into t_always_6882(a, b) values (-2, -200)");
                    assertCompileError
                      (
                       CANNOT_MODIFY_ALWAYS_IDENTITY_COLUMN,
                       "insert into t_default_6882(a, b) values (-2, -200)"
                       );
                    s.execute("insert into t_always_6882(b) values (200)");
                    s.execute("insert into t_default_6882(b) values (200)");
                    JDBC.assertFullResultSet(
                        s.executeQuery("select * from t_always_6882 order by a"),
                        new String[][]
                        {
                          { "-2", "-200" },
                          { "1", "100" },
                          { "2", "200" },
                        });
                    JDBC.assertFullResultSet(
                        s.executeQuery("select * from t_default_6882 order by a"),
                        new String[][]
                        {
                          { "-1", "-100" },
                          { "1", "100" },
                          { "2", "200" },
                        });
                } else {
                    assertCompileError
                      (
                       UPGRADE_REQUIRED,
                       "alter table t_none_6882 alter column a set generated by default"
                       );
                    assertCompileError
                      (
                       UPGRADE_REQUIRED,
                       "alter table t_always_6882 alter column a set generated by default"
                       );
                    assertCompileError
                      (
                       UPGRADE_REQUIRED,
                       "alter table t_default_6882 alter column a set generated always"
                       );
                }
                break;
                
            case PH_HARD_UPGRADE:
                assertCompileError
                  (
                   CANNOT_ALTER_NON_IDENTITY_COLUMN,
                   "alter table t_none_6882 alter column a set generated by default"
                   );
                s.execute("alter table t_always_6882 alter column a set generated always");
                s.execute("alter table t_default_6882 alter column a set generated by default");
                assertCompileError
                  (
                   CANNOT_MODIFY_ALWAYS_IDENTITY_COLUMN,
                   "insert into t_always_6882(a, b) values (-3, -300)"
                   );
                s.execute("insert into t_default_6882(a, b) values (-3, -300)");
                s.execute("insert into t_always_6882(b) values (300)");
                s.execute("insert into t_default_6882(b) values (300)");

                String[][] alwaysResults;
                String[][] defaultResults;
                if (atLeast10_11)
                {
                    alwaysResults =  new String[][]  
                      {
                        { "-2", "-200" },
                        { "1", "100" },
                        { "2", "200" },
                        { "3", "300" },
                      };
                    defaultResults = new String[][]
                      {
                        { "-3", "-300" },
                        { "-1", "-100" },
                        { "1", "100" },
                        { "2", "200" },
                        { "3", "300" },
                      };
                }
                else
                {
                    alwaysResults = new String[][]  
                      {
                        { "1", "100" },
                        { "2", "300" },
                      };
                    defaultResults = new String[][]
                      {
                        { "-3", "-300" },
                        { "-1", "-100" },
                        { "1", "100" },
                        { "2", "300" },
                      };
                }

                JDBC.assertFullResultSet
                  (
                   s.executeQuery("select * from t_always_6882 order by a"),
                   alwaysResults
                   );
                JDBC.assertFullResultSet
                  (
                   s.executeQuery("select * from t_default_6882 order by a"),
                   defaultResults);
                break;
        };
    }
}