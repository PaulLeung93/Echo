import subprocess
import argparse
import sys

def fetch_logs(lines=200, grep=None, level="E"):
    """
    Fetches the last N lines of logcat, optionally filtering by string or log level.
    """
    # Base command: dump (-d), last N lines (-t), specific format (-v)
    cmd = ["adb", "logcat", "-d", "-t", str(lines), "-v", "time"]

    # Filter by level (V, D, I, W, E, F)
    # This filters output to show only priority 'level' and higher for all tags
    cmd.append(f"*:{level}")

    try:
        # Run adb command
        result = subprocess.run(cmd, capture_output=True, text=True, check=False)
        
        if result.returncode != 0:
            print(f"Error running ADB: {result.stderr}")
            return

        logs = result.stdout.splitlines()

        # Apply secondary string filter (grep) if requested
        if grep:
            logs = [line for line in logs if grep.lower() in line.lower()]

        if not logs:
            print("No logs found matching criteria.")
            return

        # Print the logs for the agent to read
        print(f"--- Displaying last {len(logs)} matching lines ---")
        for line in logs:
            print(line)

    except FileNotFoundError:
        print("Error: 'adb' command not found. Ensure Android SDK platform-tools is in your PATH.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Fetch Android Logs")
    parser.add_argument("--lines", type=int, default=200, help="Number of lines to retrieve (default 200)")
    parser.add_argument("--grep", type=str, help="Filter logs by a specific string (e.g. 'Exception')")
    parser.add_argument("--level", type=str, default="E", help="Minimum Log Level (V, D, I, W, E, F). Default is E (Error).")
    
    args = parser.parse_args()
    fetch_logs(lines=args.lines, grep=args.grep, level=args.level)