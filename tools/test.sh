#!/usr/bin/env bash
# Run Android unit tests and coverage verification
set -euo pipefail

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Script directory for relative paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Help function
show_help() {
    cat << 'EOF'
Usage: test.sh [OPTIONS]

Run Android unit tests with JaCoCo coverage verification (minimum 85%).

Options:
  -h, --help       Show this help message
  -u, --unit-only  Run unit tests only, skip coverage check
  
Output:
  Test results → build/test-results/
  Coverage report → build/reports/jacoco/
  
Example:
  ./tools/test.sh           # Run tests with coverage check
  ./tools/test.sh --unit-only  # Run tests only, no coverage
EOF
    exit 0
}

# Parse arguments
SKIP_COVERAGE=false
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            ;;
        -u|--unit-only)
            SKIP_COVERAGE=true
            shift
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}" >&2
            show_help
            ;;
    esac
done

# Check prerequisites
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ Java not found. Please install JDK 11+${NC}" >&2
        return 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo -e "${YELLOW}  Java: $JAVA_VERSION${NC}"
}

# Main test flow
main() {
    echo -e "${GREEN}=== Android Test Suite ===${NC}"
    
    echo "Checking prerequisites..."
    check_java || return 1
    
    cd "$PROJECT_ROOT/src/android"
    
    echo "Running unit tests..."
    TEST_OUTPUT=$(./gradlew test 2>&1 || true)
    
    # Parse test results
    PASSED_COUNT=$(echo "$TEST_OUTPUT" | grep -oP '(?<=\d+ (passed|failed))|passed' | wc -l || echo 0)
    FAILED_COUNT=$(echo "$TEST_OUTPUT" | grep -oP 'failed' | wc -l || echo 0)
    
    if echo "$TEST_OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
        echo -e "${GREEN}✓ Unit tests passed${NC}"
    else
        echo -e "${RED}✗ Some tests failed${NC}" >&2
        echo "$TEST_OUTPUT" | tail -20
        return 1
    fi
    
    if [ "$SKIP_COVERAGE" = true ]; then
        echo -e "${YELLOW}⚠ Coverage check skipped (--unit-only flag)${NC}"
        return 0
    fi
    
    echo "Generating JaCoCo coverage report..."
    if ! ./gradlew jacocoTestReport > /dev/null 2>&1; then
        echo -e "${RED}✗ JaCoCo report generation failed${NC}" >&2
        return 1
    fi
    
    echo -e "${GREEN}✓ Coverage report generated${NC}"
    
    echo "Verifying coverage (minimum 85%)..."
    if ! ./gradlew jacocoTestCoverageVerification > /dev/null 2>&1; then
        echo -e "${RED}✗ Coverage verification failed (< 85%)${NC}" >&2
        echo "Run './gradlew jacocoTestReport' to view detailed coverage report"
        return 1
    fi
    
    echo -e "${GREEN}✓ Coverage verification passed (≥ 85%)${NC}"
    echo -e "${GREEN}✓ All tests successful${NC}"
    echo -e "${GREEN}  Report: src/android/build/reports/jacoco/test/html/index.html${NC}"
}

main "$@"
