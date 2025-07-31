    /**
     * 従業員IDによる検索
     * @param employeeId 従業員ID
     * @return 従業員情報
     */
    public User findEmployeeByEmployeeId(String employeeId) {
        log.info("従業員ID検索: employeeId={}", employeeId);
        
        // 修正: findByEmployeeIdメソッドが存在しないため、IDで検索するように変更
        // 明示的な変換を使用してStringからLongへ変換
        return userRepository.findById(Long.valueOf(employeeId))
            .orElseThrow(() -> new IllegalArgumentException("従業員が見つかりません: " + employeeId));
    }