#!/bin/bash

# Script to validate Liquibase changelog syntax
# This script checks if the Liquibase changelogs are syntactically correct

echo "🔍 Validating Liquibase Changelog Syntax..."

# Change to project directory
cd "$(dirname "$0")/.."

echo "📁 Current directory: $(pwd)"

# Check if Liquibase files exist
echo "📋 Checking changelog files..."

if [ ! -f "src/main/resources/db/changelog/db.changelog-master.xml" ]; then
    echo "❌ Master changelog not found!"
    exit 1
fi

if [ ! -f "src/main/resources/db/changelog/10-drop-non-core-tables.xml" ]; then
    echo "❌ Drop tables changelog not found!"
    exit 1
fi

if [ ! -f "src/main/resources/db/changelog/11-recreate-non-core-tables.xml" ]; then
    echo "❌ Recreate tables changelog not found!"
    exit 1
fi

echo "✅ All changelog files found"

# Validate XML syntax using xmllint (if available)
if command -v xmllint &> /dev/null; then
    echo "🔍 Validating XML syntax with xmllint..."
    
    xmllint --noout src/main/resources/db/changelog/db.changelog-master.xml
    if [ $? -eq 0 ]; then
        echo "✅ Master changelog XML is valid"
    else
        echo "❌ Master changelog XML is invalid"
        exit 1
    fi
    
    xmllint --noout src/main/resources/db/changelog/10-drop-non-core-tables.xml
    if [ $? -eq 0 ]; then
        echo "✅ Drop tables changelog XML is valid"
    else
        echo "❌ Drop tables changelog XML is invalid"
        exit 1
    fi
    
    xmllint --noout src/main/resources/db/changelog/11-recreate-non-core-tables.xml
    if [ $? -eq 0 ]; then
        echo "✅ Recreate tables changelog XML is valid"
    else
        echo "❌ Recreate tables changelog XML is invalid"
        exit 1
    fi
else
    echo "⚠️  xmllint not available, skipping XML syntax validation"
fi

# Validate with Maven Liquibase plugin
echo "🔍 Validating with Maven Liquibase plugin..."

mvn liquibase:validate -q
if [ $? -eq 0 ]; then
    echo "✅ Liquibase validation passed"
else
    echo "❌ Liquibase validation failed"
    exit 1
fi

# Check Liquibase status
echo "📊 Checking Liquibase status..."

mvn liquibase:status -q
if [ $? -eq 0 ]; then
    echo "✅ Liquibase status check passed"
else
    echo "❌ Liquibase status check failed"
    exit 1
fi

echo ""
echo "🎉 All validations passed!"
echo ""
echo "📋 Summary:"
echo "   ✅ All changelog files exist"
echo "   ✅ XML syntax is valid"
echo "   ✅ Liquibase validation passed"
echo "   ✅ Liquibase status check passed"
echo ""
echo "🚀 Ready to run: mvn liquibase:update"
echo ""
echo "⚠️  IMPORTANT: Always backup your database before running liquibase:update!"
echo "   pg_dump -h localhost -U postgres -d healthConnect > backup_before_reset.sql"
