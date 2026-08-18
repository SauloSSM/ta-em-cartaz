import './CategoryStrip.css';

const categoryShowsPng = new URL('../../../assets/ta-em-cartaz/categories/category-shows.png', import.meta.url).href;
const categoryFestivalsPng = new URL('../../../assets/ta-em-cartaz/categories/category-festivals.png', import.meta.url).href;
const categoryCulturaPng = new URL('../../../assets/ta-em-cartaz/categories/category-cultura.png', import.meta.url).href;
const categoryPertoPng = new URL('../../../assets/ta-em-cartaz/categories/category-perto-de-voce.png', import.meta.url).href;

const categories = [
  {
    title: 'SHOWS',
    subtitle: 'Música ao vivo perto de você.',
    artwork: categoryShowsPng,
  },
  {
    title: 'FESTIVAIS',
    subtitle: 'Experiências, encontros e sons.',
    artwork: categoryFestivalsPng,
  },
  {
    title: 'CULTURA',
    subtitle: 'Teatro, cinema, exposições e mais.',
    artwork: categoryCulturaPng,
  },
  {
    title: 'PERTO DE VOCÊ',
    subtitle: 'Música ao vivo perto de você.',
    artwork: categoryPertoPng,
  },
] as const;

export function CategoryStrip() {
  return (
    <section
      id="tc-category-strip"
      className="tc-category-strip"
      aria-label="Destaques de programação cultural"
    >
      <div className="tc-container">
        <div className="tc-category-strip__grid">
          {categories.map((category) => (
            <div className="tc-category-card" key={category.title}>
              <img
                src={category.artwork}
                alt=""
                aria-hidden="true"
                className="tc-category-card__art-img"
              />
              <div className="tc-category-card__header">
                <h2 className="tc-category-card__title">{category.title}</h2>
                <p className="tc-category-card__subtitle">{category.subtitle}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
