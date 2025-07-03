def solution(inventory, k):
    n = len(inventory)
    if k > n:
        return 0
    
    count = 0
    
    # For each starting position
    for i in range(n):
        unique_count = {}
        unique_items = 0
        
        # Extend the window from position i
        for j in range(i, n):
            # Add current item to window
            if inventory[j] not in unique_count:
                unique_count[inventory[j]] = 0
                unique_items += 1
            unique_count[inventory[j]] += 1
            
            # If we have at least k unique items, all subarrays from i to j, j+1, ..., n-1 are valid
            if unique_items >= k:
                count += n - j
                break
    
    return count

# Debug function to see all valid subarrays
def debug_solution(inventory, k):
    n = len(inventory)
    valid_subarrays = []
    
    for i in range(n):
        for j in range(i, n):
            subarray = inventory[i:j+1]
            unique_items = len(set(subarray))
            
            if unique_items >= k:
                valid_subarrays.append((i, j, subarray, unique_items))
    
    print(f"Input: {inventory}, k={k}")
    print(f"Valid subarrays ({len(valid_subarrays)}):")
    for i, j, sub, unique in valid_subarrays:
        print(f"  [{i}..{j}] = {sub} (unique: {unique})")
    print()
    
    return len(valid_subarrays)

# Test cases based on examples
def run_tests():
    # Test Case 1: [1, 2, 1, 1] with k=2 should give 2
    result1 = solution([1, 2, 1, 1], 2)
    print(f"Test 1: [1, 2, 1, 1], k=2 → {result1}")
    
    # Test Case 2: [1, 2, 3, 4, 1] with k=3 should give 6  
    result2 = solution([1, 2, 3, 4, 1], 3)
    print(f"Test 2: [1, 2, 3, 4, 1], k=3 → {result2}")
    
    # Test Case 3: [5, 5, 5, 5] with k=1 should give 4
    result3 = solution([5, 5, 5, 5], 1)  
    print(f"Test 3: [5, 5, 5, 5], k=1 → {result3}")
    
    # Additional test cases
    print(f"Test 4: [1, 1, 1], k=2 → {solution([1, 1, 1], 2)}")
    print(f"Test 5: [1, 2, 3], k=1 → {solution([1, 2, 3], 1)}")
    print(f"Test 6: [1], k=1 → {solution([1], 1)}")
    print(f"Test 7: [1, 2, 3], k=5 → {solution([1, 2, 3], 5)}")

if __name__ == "__main__":
    run_tests() 