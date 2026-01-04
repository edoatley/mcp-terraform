#!/usr/bin/env python3
"""
Terraform MCP Validation Script

This script connects to HashiCorp's Terraform MCP Server and validates
Terraform plans against the official Terraform Registry.
"""

import json
import subprocess
import sys
import os
import re
import logging
from typing import Dict, List, Optional, Any, Tuple
from pathlib import Path

# MCP Server Docker image
MCP_SERVER_IMAGE = "hashicorp/terraform-mcp-server:latest"

# Configure logging
LOG_LEVEL = os.getenv('DEBUG', '').lower() in ('1', 'true', 'yes')
logging.basicConfig(
    level=logging.DEBUG if LOG_LEVEL else logging.INFO,
    format='%(levelname)s: %(message)s',
    stream=sys.stdout
)
logger = logging.getLogger(__name__)

# Request ID counter for JSON-RPC
_request_id_counter = 0


def get_next_request_id() -> int:
    """Get the next unique request ID for JSON-RPC."""
    global _request_id_counter
    _request_id_counter += 1
    return _request_id_counter


def send_mcp_request(method: str, params: Dict[str, Any] = None) -> Dict[str, Any]:
    """Send a JSON-RPC request to the MCP server via Docker stdio."""
    request_id = get_next_request_id()
    request = {
        "jsonrpc": "2.0",
        "id": request_id,
        "method": method
    }
    
    if params:
        request["params"] = params
    
    try:
        logger.debug(f"Sending MCP request: method={method}, id={request_id}")
        if LOG_LEVEL:
            logger.debug(f"Request params: {json.dumps(params) if params else 'None'}")
        
        process = subprocess.Popen(
            ["docker", "run", "--rm", "-i", MCP_SERVER_IMAGE],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        request_json = json.dumps(request) + "\n"
        stdout, stderr = process.communicate(input=request_json, timeout=30)
        
        if process.returncode != 0:
            logger.error(f"MCP server returned non-zero exit code {process.returncode}: {stderr}")
            return {}
        
        # Handle multi-line JSON responses
        stdout_lines = stdout.strip().split('\n')
        response_json = stdout_lines[-1] if stdout_lines else stdout.strip()
        
        try:
            response = json.loads(response_json)
            
            # Check for JSON-RPC errors
            if "error" in response:
                error = response["error"]
                error_code = error.get('code', 'unknown')
                error_message = error.get('message', 'unknown error')
                
                # Suppress non-critical errors (like notifications/initialized not found)
                if error_code == -32601 and "notifications/initialized" in error_message:
                    logger.debug(f"MCP server: {error_message} (not critical, ignoring)")
                else:
                    logger.error(f"MCP server error: {error_code} - {error_message}")
                    if "data" in error:
                        logger.debug(f"Error data: {error['data']}")
                return {}
            
            result = response.get("result", {})
            if LOG_LEVEL:
                logger.debug(f"MCP response: {json.dumps(result)[:200]}...")
            return result
            
        except json.JSONDecodeError as e:
            logger.error(f"Error parsing MCP response: {e}")
            logger.debug(f"Raw response: {stdout}")
            logger.debug(f"Stderr: {stderr}")
            return {}
            
    except subprocess.TimeoutExpired:
        logger.error("MCP server request timed out after 30 seconds")
        return {}
    except FileNotFoundError:
        logger.error("Docker not found. Please ensure Docker is installed and available in PATH.")
        return {}
    except Exception as e:
        logger.error(f"Error communicating with MCP server: {e}", exc_info=LOG_LEVEL)
        return {}


def initialize_mcp() -> bool:
    """Initialize the MCP server connection."""
    logger.info("Initializing MCP server connection...")
    params = {
        "protocolVersion": "2024-11-05",
        "clientInfo": {
            "name": "terraform-mcp-validator",
            "version": "1.0.0"
        }
    }
    
    result = send_mcp_request("initialize", params)
    
    if "capabilities" in result:
        logger.info("MCP server initialized successfully")
        # Note: Some MCP servers don't support notifications/initialized
        # This is not critical for our use case, so we ignore errors
        return True
    else:
        logger.warning("Failed to initialize MCP server - capabilities not found in response")
        return False


def get_provider_version(namespace: str, provider: str) -> Optional[str]:
    """Get the latest version of a provider from the Terraform Registry."""
    logger.debug(f"Getting latest version for provider: {namespace}/{provider}")
    
    # MCP tools are called using tools/call method
    params = {
        "name": "get_latest_provider_version",
        "arguments": {
            "namespace": namespace,
            "name": provider
        }
    }
    
    result = send_mcp_request("tools/call", params)
    if result:
        # MCP tool results are in a "content" array with text items
        if "content" in result:
            for content_item in result["content"]:
                if content_item.get("type") == "text":
                    text = content_item.get("text", "").strip()
                    if text:
                        # Version is returned as plain text (e.g., "6.27.0")
                        logger.debug(f"Found version: {text}")
                        return text
        # Fallback: check if result has version directly
        version = result.get("version")
        if version:
            logger.debug(f"Found version (direct): {version}")
            return version
    
    logger.warning(f"Unable to determine latest version for {namespace}/{provider}")
    return None


def search_modules(provider: str, limit: int = 5) -> List[Dict[str, Any]]:
    """Search for modules related to a provider."""
    logger.debug(f"Searching for modules related to: {provider}")
    
    # MCP tools are called using tools/call method
    params = {
        "name": "search_modules",
        "arguments": {
            "module_query": provider,
            "limit": limit
        }
    }
    
    result = send_mcp_request("tools/call", params)
    if result:
        # MCP tool results are in a "content" array with text items
        if "content" in result:
            for content_item in result["content"]:
                if content_item.get("type") == "text":
                    text = content_item.get("text", "")
                    # Parse text format: module_id: namespace/name/provider/version
                    modules = []
                    # Extract module blocks (between --- markers)
                    module_blocks = re.split(r'---\s*\n', text)
                    for block in module_blocks:
                        # Skip header block (contains "Available Terraform Modules")
                        if "Available Terraform Modules" in block or "Each result includes" in block:
                            continue
                        
                        # Extract module_id
                        module_id_match = re.search(r'module_id:\s*([^\n]+)', block)
                        if module_id_match:
                            module_id = module_id_match.group(1).strip()
                            # Parse module_id: namespace/name/provider/version
                            parts = module_id.split('/')
                            if len(parts) >= 2:
                                module_name = parts[1]  # e.g., "iam" from "terraform-aws-modules/iam/aws/6.2.3"
                                module_source = '/'.join(parts[:2])  # e.g., "terraform-aws-modules/iam"
                                
                                # Extract name and description
                                name_match = re.search(r'Name:\s*([^\n]+)', block)
                                desc_match = re.search(r'Description:\s*([^\n]+)', block)
                                
                                module_info = {
                                    "name": module_name,
                                    "source": module_source,
                                    "module_id": module_id
                                }
                                if name_match:
                                    display_name = name_match.group(1).strip()
                                    if display_name and display_name != "name":  # Skip header
                                        module_info["display_name"] = display_name
                                if desc_match:
                                    module_info["description"] = desc_match.group(1).strip()
                                
                                modules.append(module_info)
                    
                    if modules:
                        logger.debug(f"Found {len(modules)} modules")
                        return modules
        # Fallback: check if result has modules directly
        if "modules" in result:
            modules = result.get("modules", [])
            logger.debug(f"Found {len(modules)} modules (direct)")
            return modules
    
    logger.warning(f"No modules found for provider: {provider}")
    return []


def get_resource_docs(namespace: str, provider: str, resource: str) -> Optional[Dict[str, Any]]:
    """Get documentation for a specific resource."""
    logger.debug(f"Getting docs for resource: {namespace}/{provider}/{resource}")
    
    # MCP tools are called using tools/call method
    params = {
        "name": "get_resource_docs",
        "arguments": {
            "namespace": namespace,
            "name": provider,
            "resource": resource
        }
    }
    
    result = send_mcp_request("tools/call", params)
    if result:
        # MCP tool results are typically in a "content" array with text items
        if "content" in result:
            for content_item in result["content"]:
                if content_item.get("type") == "text":
                    text = content_item.get("text", "")
                    try:
                        return json.loads(text)
                    except json.JSONDecodeError:
                        return {"text": text}
        return result
    
    return None


def normalize_provider_name(name: str) -> str:
    """Normalize a provider name to its base form.
    
    Examples:
    - "aws" -> "aws"
    - "aws.us-east-1" -> "aws"
    - "aws_api_gateway" -> "aws" (extract base provider from resource type-like names)
    """
    # Remove region/alias suffixes (e.g., "aws.us-east-1" -> "aws")
    base_name = name.split('.')[0] if '.' in name else name
    
    # If it looks like a resource type (has underscores), extract the first part
    # This handles cases where resource types might be incorrectly used as provider names
    if '_' in base_name:
        parts = base_name.split('_')
        # Only use the first part if it's a valid provider name (2-10 chars, lowercase)
        if len(parts[0]) >= 2 and len(parts[0]) <= 10 and parts[0].islower():
            return parts[0]
    
    return base_name


def extract_providers_from_terraform_files(terraform_dir: str = "terraform") -> List[Dict[str, str]]:
    """Extract providers from Terraform files by parsing required_providers blocks."""
    providers = []
    provider_sources = {}
    
    terraform_path = Path(terraform_dir)
    if not terraform_path.exists():
        logger.warning(f"Terraform directory not found: {terraform_dir}")
        return providers
    
    # Pattern to match required_providers blocks
    # Matches: provider_name = { source = "namespace/provider", version = "..." }
    provider_pattern = re.compile(
        r'(\w+)\s*=\s*\{[^}]*source\s*=\s*["\']([^"\']+)["\']',
        re.MULTILINE | re.DOTALL
    )
    
    for tf_file in terraform_path.glob("*.tf"):
        try:
            with open(tf_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Look for required_providers block
            if 'required_providers' in content:
                # Extract the required_providers block
                required_providers_match = re.search(
                    r'required_providers\s*\{([^}]+)\}',
                    content,
                    re.MULTILINE | re.DOTALL
                )
                
                if required_providers_match:
                    providers_block = required_providers_match.group(1)
                    matches = provider_pattern.findall(providers_block)
                    
                    for provider_name, source in matches:
                        if provider_name not in provider_sources:
                            # Parse source: "hashicorp/aws" -> namespace="hashicorp", name="aws"
                            source_parts = source.split('/')
                            if len(source_parts) == 2:
                                namespace = source_parts[0]
                                name = source_parts[1]
                            else:
                                # Fallback: assume hashicorp namespace
                                namespace = "hashicorp"
                                name = provider_name
                            
                            provider_sources[provider_name] = {
                                "namespace": namespace,
                                "name": name
                            }
                            logger.debug(f"Found provider in {tf_file.name}: {namespace}/{name}")
        except Exception as e:
            logger.warning(f"Error parsing {tf_file}: {e}")
    
    providers = list(provider_sources.values())
    if providers:
        logger.info(f"Extracted {len(providers)} provider(s) from Terraform files")
    
    return providers


def extract_providers_from_resource_types(plan: Dict[str, Any]) -> List[Dict[str, str]]:
    """Extract provider names from resource types in the plan.
    
    This function extracts the base provider name from resource types.
    For example: "aws_s3_bucket" -> "aws", "aws_api_gateway_rest_api" -> "aws"
    """
    providers = set()
    provider_list = []
    
    resource_changes = plan.get("resource_changes", [])
    planned_values = plan.get("planned_values", {})
    root_module = planned_values.get("root_module", {})
    
    # Extract from resource_changes
    for change in resource_changes:
        resource_type = change.get("type", "")
        if resource_type:
            # Extract provider prefix: "aws_s3_bucket" -> "aws"
            # Only match the first word before underscore to get base provider name
            provider_match = re.match(r'^([a-z][a-z0-9]*)_', resource_type)
            if provider_match:
                provider_name = normalize_provider_name(provider_match.group(1))
                if provider_name not in providers:
                    providers.add(provider_name)
                    # Default to hashicorp namespace
                    provider_list.append({
                        "namespace": "hashicorp",
                        "name": provider_name
                    })
                    logger.debug(f"Extracted provider '{provider_name}' from resource type '{resource_type}'")
    
    # Extract from planned_values.root_module.resources
    for resource in root_module.get("resources", []):
        resource_type = resource.get("type", "")
        if resource_type:
            # Extract provider prefix: "aws_s3_bucket" -> "aws"
            # Only match the first word before underscore to get base provider name
            provider_match = re.match(r'^([a-z][a-z0-9]*)_', resource_type)
            if provider_match:
                provider_name = provider_match.group(1)
                if provider_name not in providers:
                    providers.add(provider_name)
                    provider_list.append({
                        "namespace": "hashicorp",
                        "name": provider_name
                    })
                    logger.debug(f"Extracted provider '{provider_name}' from resource type '{resource_type}'")
    
    if provider_list:
        unique_providers = {p['name'] for p in provider_list}
        logger.info(f"Extracted {len(unique_providers)} unique provider(s) from resource types: {', '.join(sorted(unique_providers))}")
    
    return provider_list


def validate_plan_structure(plan: Dict[str, Any], plan_path: str) -> Tuple[bool, Optional[str]]:
    """Validate that the plan JSON has a proper structure."""
    # Check if plan is empty
    if not plan or plan == {}:
        return False, "Plan JSON is empty. This usually means terraform plan failed (e.g., missing AWS credentials)."
    
    # Check for required top-level keys
    if "format_version" not in plan and "terraform_version" not in plan:
        return False, "Plan JSON appears malformed - missing format_version and terraform_version"
    
    # Check if configuration exists
    config = plan.get("configuration", {})
    if not config:
        logger.warning("Plan JSON missing 'configuration' section - plan may be incomplete")
    
    return True, None


def extract_providers_from_plan(plan_path: str, terraform_dir: str = "terraform") -> List[Dict[str, str]]:
    """Extract unique providers from a Terraform plan JSON file with multiple fallbacks."""
    providers = set()
    provider_list = []
    
    try:
        # Check if plan file exists
        if not os.path.exists(plan_path):
            logger.warning(f"Plan file not found: {plan_path}")
            # Fallback to extracting from Terraform files
            return extract_providers_from_terraform_files(terraform_dir)
        
        # Read and parse plan JSON
        with open(plan_path, 'r') as f:
            plan = json.load(f)
        
        # Validate plan structure
        is_valid, error_msg = validate_plan_structure(plan, plan_path)
        if not is_valid:
            logger.warning(f"Plan validation failed: {error_msg}")
            logger.info("This is expected if terraform plan failed (e.g., missing AWS credentials).")
            logger.info("Attempting to extract providers from Terraform files as fallback...")
            return extract_providers_from_terraform_files(terraform_dir)
        
        # Method 1: Extract from configuration.provider_configs (primary method)
        config = plan.get("configuration", {})
        provider_configs = config.get("provider_configs", {})
        
        if provider_configs:
            logger.debug("Extracting providers from configuration.provider_configs")
            for provider_key, provider_data in provider_configs.items():
                # Normalize provider key - remove region/alias suffixes (e.g., "aws.us-east-1" -> "aws")
                # Provider keys in Terraform can be like "aws", "aws.us-east-1", etc.
                base_provider_key = normalize_provider_name(provider_key)
                
                # Try to get namespace from provider config if available
                namespace = "hashicorp"  # Default
                name = base_provider_key
                
                # Check if provider_data has source information
                if isinstance(provider_data, dict):
                    # Some plans may have full_provider_name or source
                    full_name = provider_data.get("full_provider_name", "")
                    if full_name and '/' in full_name:
                        namespace, name = full_name.split('/', 1)
                        # Normalize name to base provider
                        name = normalize_provider_name(name)
                    # Also check for name field in provider_data
                    elif "name" in provider_data:
                        name = normalize_provider_name(provider_data.get("name", base_provider_key))
                
                provider_key_full = f"{namespace}/{name}"
                if provider_key_full not in providers:
                    providers.add(provider_key_full)
                    provider_list.append({
                        "namespace": namespace,
                        "name": name
                    })
                    logger.debug(f"Found provider in plan: {namespace}/{name}")
        
        # Method 2: Extract from resource types (always run to ensure we have all providers)
        # This should only extract the base provider name (e.g., "aws" from "aws_s3_bucket")
        # We merge results with Method 1 to ensure completeness
        logger.debug("Extracting providers from resource types as additional validation...")
        resource_providers = extract_providers_from_resource_types(plan)
        for provider in resource_providers:
            provider_key = f"{provider['namespace']}/{provider['name']}"
            if provider_key not in providers:
                providers.add(provider_key)
                provider_list.append(provider)
                logger.debug(f"Added provider from resource types: {provider_key}")
        
        # Method 3: Fallback to Terraform files if still no providers
        if not provider_list:
            logger.info("No providers found in plan, extracting from Terraform files...")
            file_providers = extract_providers_from_terraform_files(terraform_dir)
            for provider in file_providers:
                provider_key = f"{provider['namespace']}/{provider['name']}"
                if provider_key not in providers:
                    providers.add(provider_key)
                    provider_list.append(provider)
        
        # Final deduplication and normalization pass
        final_providers = {}
        for provider in provider_list:
            # Normalize the provider name one more time to be safe
            normalized_name = normalize_provider_name(provider["name"])
            provider_key = f"{provider['namespace']}/{normalized_name}"
            
            if provider_key not in final_providers:
                final_providers[provider_key] = {
                    "namespace": provider["namespace"],
                    "name": normalized_name
                }
        
        final_list = list(final_providers.values())
        
        if final_list:
            unique_names = [p['name'] for p in final_list]
            logger.info(f"Successfully extracted {len(final_list)} unique provider(s) from plan: {', '.join(sorted(unique_names))}")
        else:
            logger.warning("No providers found using any extraction method")
        
        return final_list
        
    except FileNotFoundError:
        logger.warning(f"Plan file not found: {plan_path}")
        return extract_providers_from_terraform_files(terraform_dir)
    except json.JSONDecodeError as e:
        logger.error(f"Error parsing plan file: {e}")
        logger.info("Attempting to extract providers from Terraform files as fallback...")
        return extract_providers_from_terraform_files(terraform_dir)
    except Exception as e:
        logger.error(f"Error extracting providers: {e}", exc_info=LOG_LEVEL)
        return extract_providers_from_terraform_files(terraform_dir)


def analyze_plan_resources(plan_path: str) -> Dict[str, Any]:
    """Analyze resources in the Terraform plan."""
    try:
        with open(plan_path, 'r') as f:
            plan = json.load(f)
        
        resource_changes = plan.get("resource_changes", [])
        
        actions = {
            "create": 0,
            "update": 0,
            "delete": 0,
            "replace": 0
        }
        
        resources = []
        resource_types = set()
        
        for change in resource_changes:
            actions_list = change.get("change", {}).get("actions", [])
            for action in actions_list:
                if action in actions:
                    actions[action] += 1
            
            resource_type = change.get("type", "unknown")
            resource_name = change.get("name", "unknown")
            resource_types.add(resource_type)
            
            resources.append({
                "type": resource_type,
                "name": resource_name,
                "actions": actions_list
            })
        
        return {
            "actions": actions,
            "resources": resources,
            "total_resources": len(resources),
            "resource_types": sorted(list(resource_types))
        }
        
    except Exception as e:
        logger.error(f"Error analyzing plan: {e}", exc_info=LOG_LEVEL)
        return {
            "actions": {},
            "resources": [],
            "total_resources": 0,
            "resource_types": []
        }


def validate_resource_configuration(namespace: str, provider: str, resource_type: str) -> Optional[Dict[str, Any]]:
    """Validate a resource type configuration using MCP server."""
    logger.debug(f"Validating resource type: {resource_type}")
    
    # Extract provider from resource type (e.g., "aws_s3_bucket" -> "aws")
    provider_match = re.match(r'^(\w+)_', resource_type)
    if not provider_match:
        return None
    
    # Get resource documentation
    resource_name = resource_type.replace(f"{provider_match.group(1)}_", "")
    docs = get_resource_docs(namespace, provider_match.group(1), resource_name)
    
    if docs:
        return {
            "resource_type": resource_type,
            "documentation_available": True,
            "docs": docs
        }
    
    return {
        "resource_type": resource_type,
        "documentation_available": False
    }


def analyze_resource_best_practices(resources: List[Dict[str, Any]], providers: List[Dict[str, str]]) -> List[str]:
    """Analyze resources for common best practices and issues."""
    recommendations = []
    
    # Group resources by type
    resource_by_type = {}
    for resource in resources:
        resource_type = resource.get("type", "unknown")
        if resource_type not in resource_by_type:
            resource_by_type[resource_type] = []
        resource_by_type[resource_type].append(resource)
    
    # Check for common AWS resource patterns
    aws_provider = next((p for p in providers if p.get("name") == "aws"), None)
    if aws_provider:
        # Check for S3 buckets without versioning
        if "aws_s3_bucket" in resource_by_type:
            s3_resources = resource_by_type["aws_s3_bucket"]
            recommendations.append(f"Found {len(s3_resources)} S3 bucket(s) - ensure versioning and encryption are configured")
        
        # Check for Lambda functions
        if "aws_lambda_function" in resource_by_type:
            lambda_resources = resource_by_type["aws_lambda_function"]
            recommendations.append(f"Found {len(lambda_resources)} Lambda function(s) - review timeout and memory settings")
        
        # Check for DynamoDB tables
        if "aws_dynamodb_table" in resource_by_type:
            dynamodb_resources = resource_by_type["aws_dynamodb_table"]
            recommendations.append(f"Found {len(dynamodb_resources)} DynamoDB table(s) - verify backup and encryption settings")
        
        # Check for IAM roles
        if "aws_iam_role" in resource_by_type:
            iam_resources = resource_by_type["aws_iam_role"]
            recommendations.append(f"Found {len(iam_resources)} IAM role(s) - ensure least-privilege policies are applied")
    
    return recommendations


def check_plan_generation_status(plan_path: str, plan_output_path: str = "terraform/plan_output.txt") -> Tuple[bool, Optional[str]]:
    """Check if terraform plan was successful by examining output file."""
    if not os.path.exists(plan_output_path):
        return True, None  # Can't determine, assume OK
    
    try:
        with open(plan_output_path, 'r') as f:
            output = f.read()
        
        # Check for common error indicators
        error_patterns = [
            r'Error:',
            r'Error configuring',
            r'Failed to',
            r'authentication',
            r'credentials',
            r'AccessDenied'
        ]
        
        for pattern in error_patterns:
            if re.search(pattern, output, re.IGNORECASE):
                return False, f"Terraform plan appears to have failed. Check {plan_output_path} for details."
        
        return True, None
    except Exception as e:
        logger.debug(f"Could not read plan output file: {e}")
        return True, None  # Can't determine, assume OK


def main():
    """Main validation function."""
    plan_path = "tfplan.json"
    terraform_dir = "terraform"
    
    # Validate plan file exists
    if not os.path.exists(plan_path):
        logger.error(f"Plan file not found: {plan_path}")
        logger.error("Please ensure terraform plan was run successfully.")
        sys.exit(1)
    
    # Check plan generation status
    plan_status_ok, plan_error = check_plan_generation_status(plan_path)
    if not plan_status_ok and plan_error:
        logger.warning(plan_error)
        logger.info("Validation will continue using fallback methods (extracting providers from Terraform files).")
    
    # Initialize MCP server
    logger.info("Initializing MCP server connection...")
    if not initialize_mcp():
        logger.error("Failed to initialize MCP server. Validation cannot continue.")
        logger.error("This may indicate:")
        logger.error("  - Docker is not available or not running")
        logger.error("  - MCP server image failed to start")
        logger.error("  - Network connectivity issues")
        sys.exit(1)
    
    # Extract providers
    logger.info("Extracting providers from Terraform plan...")
    providers = extract_providers_from_plan(plan_path, terraform_dir)
    
    if not providers:
        logger.warning("No providers found in plan or Terraform files")
        logger.warning("This may indicate:")
        logger.warning("  - Terraform plan failed (check plan_output.txt)")
        logger.warning("  - No providers defined in Terraform configuration")
        logger.warning("  - Plan file is empty or malformed")
        # Don't exit - continue with empty provider list for reporting
    
    # Analyze plan
    logger.info("Analyzing Terraform plan...")
    plan_analysis = analyze_plan_resources(plan_path)
    
    # Check if plan has actual resources (not empty)
    plan_has_resources = plan_analysis['total_resources'] > 0
    
    # Generate validation report
    report_lines = [
        "# Terraform MCP Validation Report\n",
        f"## Plan Summary\n",
        f"- Total Resources: {plan_analysis['total_resources']}\n",
        f"- Actions:\n",
        f"  - Create: {plan_analysis['actions'].get('create', 0)}\n",
        f"  - Update: {plan_analysis['actions'].get('update', 0)}\n",
        f"  - Delete: {plan_analysis['actions'].get('delete', 0)}\n",
        f"  - Replace: {plan_analysis['actions'].get('replace', 0)}\n",
    ]
    
    # Add resource types if plan has resources
    if plan_has_resources and plan_analysis.get('resource_types'):
        report_lines.append(f"\n### Resource Types in Plan\n")
        report_lines.append(f"- {len(plan_analysis['resource_types'])} unique resource type(s):\n")
        for resource_type in plan_analysis['resource_types'][:10]:  # Limit to first 10
            report_lines.append(f"  - `{resource_type}`\n")
        if len(plan_analysis['resource_types']) > 10:
            report_lines.append(f"  - ... and {len(plan_analysis['resource_types']) - 10} more\n")
        report_lines.append("\n")
    
    report_lines.append("## Provider Validation\n")
    
    if not providers:
        report_lines.append("⚠️ **Note**: No providers found in plan.\n\n")
        report_lines.append("This usually indicates that `terraform plan` failed (e.g., missing AWS credentials).\n")
        report_lines.append("Provider validation was attempted using fallback methods but no providers were found.\n\n")
    else:
        for provider in providers:
            namespace = provider["namespace"]
            name = provider["name"]
            
            logger.info(f"Validating provider: {namespace}/{name}")
            
            version = get_provider_version(namespace, name)
            modules = search_modules(name, limit=3)
            
            report_lines.append(f"### {namespace}/{name}\n")
            
            if version:
                report_lines.append(f"- Latest Version: `{version}`\n")
            else:
                report_lines.append(f"- Latest Version: ⚠️ Unable to determine\n")
            
            if modules:
                report_lines.append(f"- Recommended Modules:\n")
                for module in modules[:3]:
                    # Prefer display_name if available, otherwise use name
                    module_name = module.get("display_name") or module.get("name", "unknown")
                    module_source = module.get("source", "")
                    if module_source:
                        report_lines.append(f"  - `{module_name}` ({module_source})\n")
                    else:
                        report_lines.append(f"  - `{module_name}`\n")
            else:
                report_lines.append(f"- Recommended Modules: ⚠️ None found\n")
            
            report_lines.append("\n")
    
    # Add resource-level analysis if plan has resources
    if plan_has_resources and providers:
        report_lines.append("## Resource Analysis\n")
        
        # Analyze best practices
        recommendations = analyze_resource_best_practices(plan_analysis['resources'], providers)
        if recommendations:
            report_lines.append("### Recommendations\n")
            for rec in recommendations:
                report_lines.append(f"- {rec}\n")
            report_lines.append("\n")
        
        # Show resource changes summary
        if plan_analysis['resources']:
            report_lines.append("### Resource Changes\n")
            # Group by action type
            by_action = {"create": [], "update": [], "delete": [], "replace": []}
            for resource in plan_analysis['resources']:
                actions = resource.get("actions", [])
                for action in actions:
                    if action in by_action:
                        by_action[action].append(resource)
            
            for action, resources_list in by_action.items():
                if resources_list:
                    report_lines.append(f"\n**{action.upper()}** ({len(resources_list)} resource(s)):\n")
                    for resource in resources_list[:5]:  # Show first 5
                        report_lines.append(f"- `{resource['type']}.{resource['name']}`\n")
                    if len(resources_list) > 5:
                        report_lines.append(f"- ... and {len(resources_list) - 5} more\n")
            report_lines.append("\n")
    
    # Write report
    report_content = "".join(report_lines)
    with open("mcp_validation_report.txt", "w") as f:
        f.write(report_content)
    
    logger.info("Validation complete!")
    logger.info("Report written to: mcp_validation_report.txt")
    
    # Create AI analysis
    if plan_analysis['total_resources'] > 0:
        resource_summary = f"{plan_analysis['total_resources']} resource changes"
        action_summary = []
        if plan_analysis['actions'].get('create', 0) > 0:
            action_summary.append(f"{plan_analysis['actions']['create']} to create")
        if plan_analysis['actions'].get('update', 0) > 0:
            action_summary.append(f"{plan_analysis['actions']['update']} to update")
        if plan_analysis['actions'].get('delete', 0) > 0:
            action_summary.append(f"{plan_analysis['actions']['delete']} to delete")
        if plan_analysis['actions'].get('replace', 0) > 0:
            action_summary.append(f"{plan_analysis['actions']['replace']} to replace")
        
        action_text = ", ".join(action_summary) if action_summary else "no changes"
        
        ai_analysis = f"""## Terraform Plan Analysis

This plan includes:
- {resource_summary} ({action_text})
- {len(plan_analysis.get('resource_types', []))} unique resource type(s)
- Provider validation completed via HashiCorp MCP Server

### Infrastructure Changes:
"""
        if plan_analysis.get('resource_types'):
            ai_analysis += "- Resource types: " + ", ".join(plan_analysis['resource_types'][:5])
            if len(plan_analysis['resource_types']) > 5:
                ai_analysis += f", and {len(plan_analysis['resource_types']) - 5} more"
            ai_analysis += "\n"
        
        ai_analysis += """
### Recommendations:
1. Review provider versions to ensure you're using the latest stable versions
2. Consider using recommended modules from the Terraform Registry
3. Verify all resource configurations match the latest provider documentation
4. Review resource changes carefully before applying
"""
        
        # Add best practice recommendations
        if providers:
            recommendations = analyze_resource_best_practices(plan_analysis['resources'], providers)
            if recommendations:
                ai_analysis += "\n### Best Practices:\n"
                for rec in recommendations:
                    ai_analysis += f"- {rec}\n"
    else:
        resource_summary = "No resource changes (plan may be empty or failed)"
        ai_analysis = f"""## Terraform Plan Analysis

This plan includes:
- {resource_summary}
- Provider validation completed via HashiCorp MCP Server

### Recommendations:
1. Review provider versions to ensure you're using the latest stable versions
2. Consider using recommended modules from the Terraform Registry
3. Verify all resource configurations match the latest provider documentation

*Note: If the plan is empty, this may indicate missing AWS credentials. Configure AWS OIDC authentication to enable full plan generation.*
"""
    
    with open("ai_analysis.txt", "w") as f:
        f.write(ai_analysis)
    
    logger.info("AI analysis written to: ai_analysis.txt")


if __name__ == "__main__":
    main()
