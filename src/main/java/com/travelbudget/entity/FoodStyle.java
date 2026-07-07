package com.travelbudget.entity;

/**
 * Стиль питания в поездке — влияет на оценку расходов на еду.
 *  SELF_CATERING — сам готовлю / магазины (дёшево);
 *  CAFES         — кафе и стрит-фуд (средне);
 *  RESTAURANTS   — рестораны (дорого);
 *  MIXED         — как получится.
 */
public enum FoodStyle {
    SELF_CATERING,
    CAFES,
    RESTAURANTS,
    MIXED
}
