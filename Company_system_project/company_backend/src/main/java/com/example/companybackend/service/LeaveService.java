    /**
     * ユーザーの承認済み休暇日数取得
     * @param userId ユーザーID
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 承認済み休暇日数
     */
    @Transactional(readOnly = true)
    public long getApprovedLeaveDays(Long userId, LocalDate startDate, LocalDate endDate) {
        return leaveRequestRepository.countApprovedDaysInPeriod(userId, startDate, endDate);
    }