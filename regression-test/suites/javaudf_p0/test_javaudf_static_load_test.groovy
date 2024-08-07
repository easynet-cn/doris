// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import org.codehaus.groovy.runtime.IOGroovyMethods

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

suite("test_javaudf_static_load_test") {
    def tableName = "test_javaudf_static_load_test"
    def jarPath = """${context.file.parent}/jars/java-udf-case-jar-with-dependencies.jar"""


    log.info("Jar path: ${jarPath}".toString())
    try {
        sql """ DROP TABLE IF EXISTS ${tableName} """
        sql """
        CREATE TABLE IF NOT EXISTS ${tableName} (
            `user_id`     INT         NOT NULL COMMENT "用户id",
            `char_col`    CHAR        NOT NULL COMMENT "",
            `varchar_col` VARCHAR(10) NOT NULL COMMENT "",
            `string_col`  STRING      NOT NULL COMMENT ""
            )
            DISTRIBUTED BY HASH(user_id) PROPERTIES("replication_num" = "1");
        """
        StringBuilder sb = new StringBuilder()
        int i = 1
        for (; i < 9; i ++) {
            sb.append("""
                (${i % 3}, '${i}','abcdefg${i}','poiuytre${i}abcdefg'),
            """)
        }
        sb.append("""
                (${i}, '${i}','abcdefg${i}','poiuytre${i}abcdefg')
            """)
        sql """ INSERT INTO ${tableName} VALUES
             ${sb.toString()}
            """
        qt_select_default """ SELECT * FROM ${tableName} t ORDER BY char_col; """

        File path = new File(jarPath)
        if (!path.exists()) {
            throw new IllegalStateException("""${jarPath} doesn't exist! """)
        }

        sql """ CREATE FUNCTION static_load_test() RETURNS int PROPERTIES (
            "file"="file://${jarPath}",
            "symbol"="org.apache.doris.udf.StaticIntTest",
            "static_load"="true",
            "expiration_time"="10",
            "type"="JAVA_UDF"
        ); """

        sql """set parallel_pipeline_task_num = 1; """
        qt_select1 """ SELECT static_load_test(); """
        qt_select2 """ SELECT static_load_test(); """
        qt_select3 """ SELECT static_load_test(); """
        qt_select4 """ SELECT static_load_test(); """
        qt_select5 """ SELECT static_load_test(); """

    } finally {
        try_sql("DROP FUNCTION IF EXISTS static_load_test();")
        try_sql("DROP TABLE IF EXISTS ${tableName}")
    }
}

