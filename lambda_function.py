import boto3

def find_unallocated_eips(regions_to_check):
    """
    Finds and prints unallocated Elastic IPs across all AWS regions.
    """
    # Use region list provided if possible else use all
    if regions_to_check:
        regions = regions_to_check
    else:
        ec2 = boto3.client('ec2', region_name='us-east-1') # Start with a default region to list all
        regions = [region['RegionName'] for region in ec2.describe_regions()['Regions']]

    print(f'Searching for unused IPs in regions {regions}')

    unallocated_ips = []

    for region in regions:
        print(f"Checking region: {region}")
        ec2_regional = boto3.client('ec2', region_name=region)
        
        # Describe all Elastic IPs in the region
        try:
            addresses = ec2_regional.describe_addresses()['Addresses']
            for address in addresses:
                # An EIP is unallocated if it does not have an AssociationId
                if 'AssociationId' not in address:
                    unallocated_ips.append({
                        'Region': region,
                        'PublicIp': address['PublicIp'],
                        'AllocationId': address['AllocationId'],
                        'Domain': address.get('Domain', 'vpc') # Default to vpc if not specified
                    })
        except Exception as e:
            # Handle cases where the user might not have permissions for a region
            print(f"Could not describe addresses in {region}: {e}")

    return unallocated_ips

def lambda_handler(event, context):
    used_regions=['us-east-1', 'eu-west-2']
    # Find all unallocated IPs across all regions (no need to loop - find_unallocated_eips already handles all regions)
    unallocated_ips = find_unallocated_eips(used_regions)
    
    for ip in unallocated_ips:
        if 'release' in event and event['release'] == 'Y':
            # Example: {'Region': 'us-east-1', 'PublicIp': '98.87.203.25', 'AllocationId': 'eipalloc-0ef4c7c130f327fce', 'Domain': 'vpc'}
            ec2_regional = boto3.client('ec2', region_name=ip['Region'])
            ec2_regional.release_address(AllocationId=ip['AllocationId'])
        else:
            print(f'Dry-run, would have removed IP {ip["PublicIp"]}, allocation ID: {ip["AllocationId"]}')

    return {"statusCode": 200}

