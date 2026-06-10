import sys
import requests
import json
import argparse

def search_maven(query, limit=5):
    """
    Searches Maven Central for the given query (e.g., 'retrofit', 'hilt-android').
    """
    url = "https://search.maven.org/solrsearch/select"
    params = {
        "q": query,
        "rows": limit,
        "wt": "json"
    }

    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        
        docs = data.get('response', {}).get('docs', [])
        
        if not docs:
            print(f"No results found for query: {query}")
            return

        print(f"Found {len(docs)} results for '{query}':\n")
        
        for doc in docs:
            group = doc.get('g')
            artifact = doc.get('a')
            latest_version = doc.get('latestVersion')
            
            # Format as Gradle Kotlin DSL string for easy copy-paste
            print(f"--- {group}:{artifact} ---")
            print(f"Latest Version: {latest_version}")
            print(f"Gradle (KTS): implementation(\"{group}:{artifact}:{latest_version}\")")
            print(f"Toml Format:  {artifact.replace('-', '_')} = {{ group = \"{group}\", name = \"{artifact}\", version = \"{latest_version}\" }}")
            print("")

    except Exception as e:
        print(f"Error searching Maven: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Search Maven Central.')
    parser.add_argument('query', type=str, help='Library name to search for')
    args = parser.parse_args()
    
    search_maven(args.query)