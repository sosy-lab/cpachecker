def contains_subsequence(subsequence, sequence):
    l = len(subsequence)
    return any(subsequence == sequence[i:i + l] for i in range(len(sequence) - l + 1))
