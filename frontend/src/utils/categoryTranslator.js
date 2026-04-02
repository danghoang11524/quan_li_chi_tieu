// Chuyển đổi category từ tiếng Anh sang tiếng Việt
export const translateCategory = (category) => {
  const categoryMap = {
    // Expense categories
    'DINING': 'Ăn uống',
    'TRANSPORTATION': 'Di chuyển',
    'ENTERTAINMENT': 'Giải trí',
    'SHOPPING': 'Mua sắm',
    'BILLS': 'Hóa đơn',
    'UTILITIES': 'Tiện ích',
    'HEALTHCARE': 'Y tế',
    'EDUCATION': 'Giáo dục',
    'HOUSING': 'Nhà ở',
    'OTHER': 'Khác',
    'GROCERIES': 'Thực phẩm',
    'PERSONAL_CARE': 'Chăm sóc cá nhân',
    'INSURANCE': 'Bảo hiểm',
    'DEBT_PAYMENT': 'Trả nợ',
    'SAVINGS': 'Tiết kiệm',
    'INVESTMENT': 'Đầu tư',
    'GIFT': 'Quà tặng',
    'CHARITY': 'Từ thiện',
    'SUBSCRIPTION': 'Đăng ký',
    'TRAVEL': 'Du lịch',
    'FITNESS': 'Thể thao',

    // Income categories
    'SALARY': 'Lương',
    'FREELANCE': 'Tự do',
    'BUSINESS': 'Kinh doanh',
    'INVESTMENT_INCOME': 'Đầu tư',
    'RENTAL': 'Cho thuê',
    'BONUS': 'Thưởng',
    'GIFT_INCOME': 'Quà tặng',
    'OTHER_INCOME': 'Khác',
  };

  return categoryMap[category] || category;
};

// Chuyển đổi ngược từ tiếng Việt sang tiếng Anh (nếu cần)
export const translateCategoryToEnglish = (vietnameseCategory) => {
  const reverseMap = {
    'Ăn uống': 'DINING',
    'Di chuyển': 'TRANSPORTATION',
    'Giải trí': 'ENTERTAINMENT',
    'Mua sắm': 'SHOPPING',
    'Hóa đơn': 'BILLS',
    'Tiện ích': 'UTILITIES',
    'Y tế': 'HEALTHCARE',
    'Giáo dục': 'EDUCATION',
    'Nhà ở': 'HOUSING',
    'Khác': 'OTHER',
    'Thực phẩm': 'GROCERIES',
    'Chăm sóc cá nhân': 'PERSONAL_CARE',
    'Bảo hiểm': 'INSURANCE',
    'Trả nợ': 'DEBT_PAYMENT',
    'Tiết kiệm': 'SAVINGS',
    'Đầu tư': 'INVESTMENT',
    'Quà tặng': 'GIFT',
    'Từ thiện': 'CHARITY',
    'Đăng ký': 'SUBSCRIPTION',
    'Du lịch': 'TRAVEL',
    'Thể thao': 'FITNESS',
    'Lương': 'SALARY',
    'Tự do': 'FREELANCE',
    'Kinh doanh': 'BUSINESS',
    'Cho thuê': 'RENTAL',
    'Thưởng': 'BONUS',
  };

  return reverseMap[vietnameseCategory] || vietnameseCategory;
};
